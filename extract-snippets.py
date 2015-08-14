#!/usr/bin/env python

import os
import sys

def loadFile(f):
  fh = open(f)
  lines = fh.readlines()
  fh.close()
  return lines

def extractCodeSnippets(lang, lines):
  inBlock = False
  blocks = []
  block = []
  for line in lines:
    if line.startswith("```%s" % lang):
      inBlock = True
    elif inBlock and not line.startswith("```"):
      block.append(line)
    elif line.startswith("```"):
      blocks.append("".join(block))
      inBlock = False
      block = []
  return blocks

def writeSnippets(lang, dir, blocks):
  if not os.path.isdir(dir):
    os.makedirs(dir)
  i = 0
  for block in blocks:
    fh = open("%s/Snippet_%02d.%s" % (dir, i, lang), 'w')
    fh.write(block)
    fh.close()
    i += 1

def main(args):
  if len(args) < 4:
    print "Usage: %s <lang> <output-dir> <files...>" % args[0]
    sys.exit(1)

  lang = args[1]
  dir = args[2]
  for i in range(3, len(args)):
    print "processing %s..." % args[i]
    lines = loadFile(args[i])
    blocks = extractCodeSnippets(lang, lines)
    writeSnippets(lang, "%s/%s" % (dir, args[i]), blocks)

main(sys.argv)
