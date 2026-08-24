package com.example.vivo200mpprobe;

interface ICommandService {
    String exec(String command);
    int getUid();
    int getPid();
    void destroy();
}
