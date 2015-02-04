#!/usr/bin/env python2.7

import os.path
from BaseHTTPServer import HTTPServer
from CGIHTTPServer import CGIHTTPRequestHandler

# Stoppable HTTP server 
# Based on http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/336012
class StoppableHTTPServer(HTTPServer):

    def serve_forever(self):
        self.stop = False
        while not self.stop:
            self.handle_request()

class StoppableHTTPRequestHandler(CGIHTTPRequestHandler):

    def __init__(self, request, client, server):
        CGIHTTPRequestHandler.__init__(self, request, client, server)

    def is_cgi(self):
        if self.path.endswith(".cgi") or self.path.find("?") != -1:
            pos = self.path.find("?")
            if pos != -1:
                cmd = self.path[0:pos]
                query = self.path[pos:]
                self.cgi_info = (".%s" % cmd, "/%s" % query)
            else:
                self.cgi_info = (".%s" % self.path, "")
            return True 
        else:
            return False
    
    def do_QUIT(self):
        print self.cgi_directories
        self.send_response(200)
        self.end_headers()
        self.server.stop = True

def stop_server(port):
    import httplib
    conn = httplib.HTTPConnection("localhost:%d" % port)
    conn.request("QUIT", "/")
    conn.getresponse()

# Entry point
if __name__ == '__main__':
    import sys, os
    from optparse import OptionParser

    # Check arguments
    parser = OptionParser()
    parser.add_option("-p", "--port", dest="port",
        action="store", help="the port to listen on", 
        default=80, type="int", metavar="INT")
    parser.add_option("-d", "--dir", dest="dir",
        action="store", help="the web directory", 
        default=".")
    parser.add_option("-s", "--shutdown", dest="shutdown",
        action="store_true", help="shutdown the server", 
        default=False)
    (options, args) = parser.parse_args()

    # Shutdown server?
    if options.shutdown:
        stop_server(options.port)
        sys.exit(0)

    # Run from HTML directory
    os.chdir(options.dir)
    server = StoppableHTTPServer(("", options.port), 
                                 StoppableHTTPRequestHandler)
    server.serve_forever()
