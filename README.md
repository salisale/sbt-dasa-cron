## DasaBook E-mail Notification Cron Job <br />
DasaBook Cafe is a secondhand bookshop in Bangkok with over 20,000 books in stock <br />
Visit: www.dasabookcafe.com <br /> <br />

### Description
This is a cron task for sending e-mail notification of filtered entries to your mail-box at a preferred interval. 
You can filter entries of both *Daily Arrivals* and an entire *Stock* database.
For *Daily Arrivals*, a simple formatted message will be sent. For filtered *Stock*, it will be sent as an excel attachment. 
You can filter books by *Author*, *Genre* or *Keyword* which must be specified in *User Specification File*. 
A sample is uploaded on here as UserFile-Mock.txt. If there is more than one user, use the format UserFile-XXX.txt to uniquely
identify each. All emails will be sent from salisadasa [at] gmail.com.<br /><br />

**User Specification File** <br />
<a href="https://imgur.com/xm8LMNd"><img src="https://i.imgur.com/xm8LMNd.png" title="source: imgur.com" /></a><br /> <br />

### Required Set-Up
1. Create a folder in your home directory called 'dasacron' and place your *User Specification File* there; 
this is where your temporary files will be stored
2. Create a simple bash script to run the program

```
#!/bin/sh

cd /path/to/project/folder
/usr/local/bin/sbt "run Main"
```
To check that it works, execute the following command in your terminal
```
$ sh /path/to/your/bash.sh
```
3. Enter the following command in your terminal to create a crontab
```
$ sudo crontab -e
```
4. Edit the cron file and save
```
0 7 * * * sh /path/to/your/bash.sh
```
This schedules the program to run every day at 7 in the morning. It must be executed everyday regardless of your notification
interval, so that a user who, for example, wants their notification every Friday receives accumulated entries from last Friday up to
Thursday's night. <br />
To see that your cron task has been properly set,
```
$ sudo crontab -l
```
See more crontab command: https://crontab.guru <br /><br />



**Filtered Daily Arrivals** <br />
<a href="https://imgur.com/CvgfcOB"><img src="https://i.imgur.com/CvgfcOB.png" title="source: imgur.com" /></a> <br />

**Filtered Stock** <br />
<a href="https://imgur.com/xmqUBBW"><img src="https://i.imgur.com/xmqUBBW.png" title="source: imgur.com" /></a>
