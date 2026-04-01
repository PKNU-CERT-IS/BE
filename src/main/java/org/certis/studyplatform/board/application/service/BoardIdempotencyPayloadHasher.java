package org.certis.studyplatform.board.application.service;

import org.certis.studyplatform.board.presentation.dto.request.AttachmentRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardCreateRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardUpdateRequestDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class BoardIdempotencyPayloadHasher {

    public String hashCreate(BoardCreateRequestDto request) {
        String canonical = String.join("|",
                normalize(request.getTitle()),
                normalize(request.getContent()),
                normalize(request.getDescription()),
                normalize(request.getCategory()),
                normalizeAttachments(request.getAttachments())
        );
        return sha256Hex(canonical);
    }

    public String hashUpdate(BoardUpdateRequestDto request) {
        String canonical = String.join("|",
                normalize(request.getTitle()),
                normalize(request.getContent()),
                normalize(request.getDescription()),
                normalize(request.getCategory()),
                normalizeAttachments(request.getAttachments())
        );
        return sha256Hex(canonical);
    }

    private String normalizeAttachments(List<AttachmentRequestDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "[]";
        }

        List<String> normalized = new ArrayList<>();
        for (AttachmentRequestDto attachment : attachments) {
            String canonicalUrl = normalizeUrl(attachment.getAttachedUrl());
            normalized.add(String.join(":",
                    normalize(attachment.getName()),
                    normalize(attachment.getType()),
                    normalize(attachment.getSize()),
                    canonicalUrl
            ));
        }

        normalized.sort(Comparator.naturalOrder());
        return String.join(",", normalized);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        int q = url.indexOf('?');
        int h = url.indexOf('#');
        int end = url.length();
        if (q >= 0) {
            end = q;
        }
        if (h >= 0 && h < end) {
            end = h;
        }
        return url.substring(0, end);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
