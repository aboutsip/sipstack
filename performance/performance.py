__author__ = 'jonas'
import threading
import time
import subprocess
import socket
import os
import csv
import re
import traceback

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

class SIPp(threading.Thread):

    def __init__(self):
        threading.Thread.__init__(self)
        self.running = False
        self.pid = 0
        self.control_port = 8888
        self.ip = '127.0.0.1'
        self.port = 5070
        self.stat_keys = []
        self.current_rate = 1

    def run(self):
        self.running = True
        p = subprocess.Popen(['sipp', '-bg', '-trace_msg', '-trace_screen', '-sn', 'uac', '-r', str(self.current_rate), '-fd', '5', '-cp', str(self.control_port), '-p', str(self.port), '-trace_stat', '127.0.0.1:5060'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.stats_file = 'uac_%s_.csv' % p.pid
        self.tail = subprocess.Popen(['tail', '-F', self.stats_file], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        while self.running:
            try:
                self.__process_stat(self.tail.stdout.readline())
            except:
                traceback.print_exc()

    def __process_stat(self, line):
        if line.startswith('StartTime'):
            csvreader = csv.reader([line,], delimiter=';')
            for row in csvreader:
                for i in range(len(row)):
                    key = camelToSnake(row[i].translate(None, "()"))
                    self.stat_keys.append(key)
                    self.__dict__[key] = None
        else:
            csvreader = csv.reader([line,], delimiter=';')
            for row in csvreader:
                for i in range(len(row)):
                    key = self.stat_keys[i]
                    self.__dict__[key] = row[i]
        print line

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

    def quit(self):
        self.running = False
        self.sock.sendto('q', (self.ip, self.control_port))
        self.tail.kill()
        os.remove(self.stats_file)

if __name__ == '__main__':

    sipp = SIPp()
    sipp.start()
    time.sleep(1)
    for i in range(10):
        print sipp.target_rate
        print sipp.call_rate_p
        sipp.set_cps((i + 1) * 14)
        time.sleep(3)
    for i in range(10):
        print sipp.target_rate
        print sipp.call_rate_p
        time.sleep(3)
    sipp.quit()

