package com.lostfound.service;

import com.lostfound.exception.StorageException;

import java.io.File;
















public interface StorageService {

    




    String uploadImage(File file, String desiredFileName) throws StorageException;

    
    boolean isAvailable();
}
