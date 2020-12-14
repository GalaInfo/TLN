#!/usr/bin/env python
# coding: utf-8

# In[1]:


from nltk import load_parser
import subprocess


# In[2]:


parser = load_parser('res/grammar.fcfg')
sentence = input("Enter an english sentence, type \"close\" to terminate the program\n").lower()
while sentence != "close":
    tokens = sentence.split()
    try:
        parsed = False
        for tree in parser.parse(tokens):
            parsed = True
            logic = tree.label()['SEM']
            print("\nLogic clauses:")
            print(logic)
            output = subprocess.check_output(["java", "-jar", "res/NLG.jar", str(logic)[1:-1]])
            print("\nItalian sentence:")
            print("".join(map(chr,output)).split("\r\n")[-2])
            print("\n" + "*" * 75)
        if not parsed:
            print("\nUnparsable sentence: semantic structure not recognized")
    except ValueError:
        print("\nUnparsable sentence: words not recognized")
    sentence = input("\nEnter an english sentence, type close to terminate the program\n").lower()

