#!/usr/bin/env python

"""
Simple module wrapper to load settings file in order
to have it available in all modules.
"""
import yaml
import os

settings_filename = './medknow/settings.yaml'
with open(settings_filename, "r") as f:
    settings = yaml.load(f)

