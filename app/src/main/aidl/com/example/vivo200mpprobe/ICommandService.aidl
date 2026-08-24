package com.example.vivo200mpprobe;

interface ICommandService {
    int uid();
    int pid();
    byte[] runCommand(in byte[] command);
}
