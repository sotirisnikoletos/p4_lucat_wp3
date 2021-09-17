#!/usr/bin/python
# -*- coding: utf-8 -*

# !!!!!!!!!  FIRST CONFIGURE SETTINGS.YAML TO MATCH YOUR NEEDS !!!!!!!!!!
# Simple script to run the Pipeline Wrapper,


import logging
import json
from tasks import taskCoordinator
from config import settings
from utilities import time_log, log_capture_string



def main():

    try:
        TaskManager = taskCoordinator()
        TaskManager.print_pipeline()
        TaskManager.start_metamap()
        TaskManager.run()
        TaskManager.stop_metamap()
        resp = "STATUS OK!"
    except Exception, e:
        resp = "PROBLEM! %s" % e
    exit(1)

if __name__ == '__main__':
    main()
