package com.studenthub.service;

import org.springframework.core.io.Resource;

public record StoredFile(Resource content, String filename, String contentType, long size) {
}
