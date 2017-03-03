import os

def print_scan_file_status(tempfile):
    print("File path: " + tempfile + "\tSize: " + str(os.path.getsize(tempfile)))