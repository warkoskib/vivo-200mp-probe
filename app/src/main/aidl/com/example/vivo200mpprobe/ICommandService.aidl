package com.example.vivo200mpprobe;

interface ICommandService {
    int uid();
    int pid();
    String runCommand(String command);
}
