﻿# P2P-File-Sharing-Software Project

### Description
This is a P2P File sharing Project. The protocol invloves a handshake followed by a 
continuous stream of length-prefixed messages, all assumed to use the TCP Transport Protocol.
Among its interesting features, our Team has implemented the chocking-unchoking mechanisms which is the most important features 
of the P2P file sharing protocol. When the program is run and a peer is connected to at least one other peer, it will start to exchange pieces. 
A peer will terminate once it finds out that all the other peers include itself has downloaded the files completely.
The protocol will establish the file management operations between peers ensuring that all peers have
downloaded the files completely.  
##### HOW TO RUN
###### Unzipping the file
There were some issues with running locally and running on the linux machine. 
Please run the following commands with code on remote machine 
1. unzip CNT4007Project-main.zip
2. cd CNT4007Project-main.zip
3. cd src
4. mv * ..
5. cd ..
6. rmdir src

###### Start with StartRemotePeers:
1. Ensure cnt4007 file, project config files, and the peer directories that have the files to be shared are uploaded to the remote server <br />
   &emsp; -The peer directories must be next to the project config files and cnt4007 file for the program to find them
2. Compile code with command javac cnt4007/*.java
3. Have the project config files beside your StartRemotePeers.java locally <br />
   &emsp; -This is so StartRemotePeers knows the information for each peer to start the peers remotely
3. Compile StartRemotePeers.java with javac StartRemotePeers.java
4. Run StartRemotePeers with java StartRemotePeers.class
2. Enter UF Gator ID and password used to ssh into remote computers
2. Choose which config file you wish to test

###### Start from each remote host individually:
1. Ensure cnt4007 file, project config files, and the peer directories that have the files to be shared are uploaded to the remote server
   &emsp; -The peer directories must be next to the project config files and cnt4007 file for the program to find them
2. Compile code with command javac cnt4007/*.java
3. Run java cnt4007/peerProcess.class <peer id> <directory of config file> 

##### PROJECT MEMBERS
Carson Schmidt - carson.schmidt@ufl.edu <br />
Sydney McLaughlin - s.mclaughlin@ufl.edu <br />
Hyoyoung (Lucy) Jin - jinh@ufl.edu

##### VIDEO LINK
https://youtu.be/W3PRu_O2pPI
