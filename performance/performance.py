__author__ = 'jonas'
import threading
import time
import subprocess
import socket
import os
import csv
import re
import traceback
import sys

_underscorer1 = re.compile(r'(.)([A-Z][a-z]+)')
_underscorer2 = re.compile('([a-z0-9])([A-Z])')

def camelToSnake(s):
    """
    Convert camel-case to snake-case in python.

    e.g.: CamelCase -> snake_case

    Relevant StackOverflow question: http://stackoverflow.com/a/1176023/293064
    __author__ = 'Jay Taylor [@jtaylor]'
    """
    subbed = _underscorer1.sub(r'\1_\2', s)
    return _underscorer2.sub(r'\1_\2', subbed).lower()

class Top(threading.Thread):
    '''Simple wrapper around top but it will only print out the information regarding
    the pids and nothing else
    '''
    def __init__(self, pids=[], delay = 5, callback=None):
        threading.Thread.__init__(self)
        self.running = False
        self.file = file;
        self.pids = pids
        self.delay = delay
        self.callback = callback

    def run(self):
        self.running = True
        pids = ",".join(str(x) for x in self.pids)
        pattern = re.compile('^\s*(%s)\s+(.*)' % "|".join(str(x) for x in self.pids))
        self.tail = subprocess.Popen(['top', '-b', '-d', str(self.delay), '-p', pids], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        while self.running:
            try:
                line = self.tail.stdout.readline()
                result = pattern.match(line)
                if result:
                    pid = result.group(1)
                    parts = result.group(2).split()
                    info = {}
                    info['pid'] = pid
                    info['cpu'] = parts[7]
                    info['mem'] = parts[8]
                    if self.callback:
                        self.callback(info)
                    else:
                        print info
            except:
                traceback.print_exc()

    def stop(self):
        self.running = False
        self.tail.kill()

class Tail(threading.Thread):
    '''Simple wrapper around tail
    '''
    def __init__(self, file, callback=None):
        threading.Thread.__init__(self)
        self.running = False
        self.file = file;
        self.callback = callback

    def run(self):
        self.running = True
        self.tail = subprocess.Popen(['tail', '-F', self.file], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        while self.running:
            try:
                line = self.tail.stdout.readline()
                if self.callback:
                    self.callback(line)
                else:
                    print line
            except:
                traceback.print_exc()
        self.tail.kill()

    def stop(self):
        self.running = False
        self.tail.wait()

class SIPp(threading.Thread):
    '''Wrapper around sipp because sipp is annoying
    '''
    def __init__(self, attach=False):
        threading.Thread.__init__(self)
        self.running = False
        self.pid = None
        self.attach = attach
        self.control_port = 8888
        self.ip = '127.0.0.1'
        self.port = 5070
        self.stat_keys = []
        self.current_rate = 1


        self.stats = []
        self.stats_file = None

    @property
    def target_rate(self):
        """The current target rate as reported by sipp"""
        return self.stats[len(self.stats) - 1][self.target_rate_index]

    def __attach_to_running_sipp(self):
        p = subprocess.Popen(['pidof', 'sipp'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        p.wait()
        if p.returncode != 0:
            print "[ERROR] Unable to attach to a running sipp. Are you sure it is up?"
        else:
            self.pid = p.stdout.readline().strip()
            print "Attached to running sipp instance with pid %s" % self.pid

    def run(self):
        self.running = True
        if self.attach:
            self.__attach_to_running_sipp()

        if not self.pid:
            p = subprocess.Popen(['sipp', '-bg', '-trace_msg', '-trace_screen', '-sn', 'uac', '-r', str(self.current_rate), '-fd', '5', '-cp', str(self.control_port), '-p', str(self.port), '-trace_stat', '127.0.0.1:5060'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.pid = p.pid
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.stats_file = 'uac_%s_.csv' % self.pid

    def dump_stats(self, samples):
        for i in self.stats_of_interest:
            sys.stdout.write(str(samples[i] + ", "))
        print

    def __process_stat(self, line):
        if not line.startswith('StartTime'):
            csvreader = csv.reader([line,], delimiter=';')
            for row in csvreader:
                self.stats.append(row)
                self.dump_stats(row)

    def set_cps(self, cps):
        diff = cps - self.current_rate
        if diff > 0:
            print "Increasing CPS by %s to target rate %s" % (diff, cps)
            for i in range(diff / 10):
                if i > 0:
                    time.sleep(1)
                self.sock.sendto('*', (self.ip, self.control_port))

            for i in range(diff % 10):
                self.sock.sendto('+', (self.ip, self.control_port))

            self.current_rate = cps

    def stop(self):
        self.running = False
        self.sock.sendto('q', (self.ip, self.control_port))
        self.sock.close()

    def clean(self):
        '''Ask sipp to clean up all files generated during the run
        '''
        os.remove(self.stats_file)

class SipServer(threading.Thread):
    '''Process for monitoring the sip server.
    For now it will not actually start the server
    but probably will eventually
    '''

    def __init__(self, pid):
        threading.Thread.__init__(self)
        self.running = False
        self.pid = pid
        self.stats = []

    def run(self):
        self.running = True
        self.top = Top([self.pid,], callback=self.__process_top)
        self.top.start()
        while self.running:
            p = subprocess.Popen(['ps', '-p', str(self.pid)], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            p.wait()
            if p.returncode != 0:
                print 'SIP Server died'
                self.running = False
            time.sleep(1)

    def __process_top(self, info):
        print info
        self.stats.append([info['cpu'], info['mem']])

    def stop(self):
        self.running = False
        self.top.stop()

class TraficController(threading.Thread):

    def __init__(self, sipp, sut):
        threading.Thread.__init__(self)
        self.running = False
        '''
        '''
        self.sipp = sipp
        self.sut = sut
        self.sipp_stats = []
        self.sip_server_stats = []
        # self.rates = [1, 10, 20, 50, 100, 200, 300, 500, 800, 1000, 1200, 1500, 2000, 2500]
        # self.times = [10, 10, 10, 10, 10, 10, 60, 60, 60, 60, 60, 60, 60, 60]

        self.rates = [1, 10, 20, 50, 100, 200, ]
        self.times = [10, 10, 10, 10, 10, 10, ]
        self.current_index = 0
        self.last_adjustment = time.time() - self.times[0] - 1;
        self.done = False
        if (len(self.rates) != len(self.times)):
            print '[ERROR] The rates and the times are not of the same length!!!'
            exit(1)

    def run(self):
        self.sut.start()
        self.sipp.start()
        self.running = True
        time.sleep(2)

        self.tail = Tail(self.sipp.stats_file, self.__process_sipp_stats)
        self.tail.start()
        while self.running:
            if self.current_index < len(self.times):
                if self.last_adjustment + self.times[self.current_index] <= time.time():
                    cps = self.rates[self.current_index]
                    print 'setting cps to %s for %s seconds' % (cps, self.times[self.current_index])
                    self.sipp.set_cps(cps)
                    self.last_adjustment = time.time()
                    self.current_index += 1
            else:
                self.running = False
                print 'we are actually done...'
            time.sleep(1)


        self.sipp.stop()
        time.sleep(6)
        print "Test scenario is done"

    def __process_sipp_stats(self, line):
        '''SIPp dumps quite a lot of statistics so just extract out the ones
        that we find useful
        '''
        if not line.startswith('StartTime'):
            csvreader = csv.reader([line,], delimiter=';')
            for row in csvreader:
                if len(row) == 88:
                    stats = {}
                    stats['current_time'] = float(row[2].split()[2])
                    stats['target_rate'] = row[5]
                    stats['call_rate_p'] = row[6]
                    stats['total_call_created'] = row[12]
                    stats['successful_call_c'] = row[15]
                    stats['failed_call_c'] = row[17]
                    stats['retransmissions_c'] = row[49]
                    stats['responsetime_partition_10'] = row[69]
                    stats['responsetime_partition_20'] = row[70]
                    stats['responsetime_partition_30'] = row[71]
                    stats['responsetime_partition_40'] = row[72]
                    stats['responsetime_partition_50'] = row[73]
                    stats['responsetime_partition_100'] = row[74]
                    stats['responsetime_partition_150'] = row[75]
                    stats['responsetime_partition_200'] = row[76]
                    stats['responsetime_partition_500'] = row[77]
                    print stats


    def stop(self):
        self.running = False

    def current_rate(self, rate):
        '''Set the current rate as reported by sipp'''
        pass


if __name__ == '__main__':

    # top -b -d 1 -p 2063 | awk '/2063/{print $1 " " $9 " " $10}'
    # process_sipp_stats('uac_2656_.csv')
    # exit(0)

    sip_server = SipServer(10643)
    sipp = SIPp(attach=True)

    tc = TraficController(sipp, sip_server)
    tc.start()
    time.sleep(300)
    tc.stop()
    sip_server.stop()
    time.sleep(10)

