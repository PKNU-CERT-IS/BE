package org.certis.studyplatform.project.application;

import org.certis.studyplatform.project.presentation.dto.request.ProjectAttachedCreateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectCreateRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectEndRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectUpdateRequestDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ProjectIdempotencyPayloadHasher {

    public String hashCreate(ProjectCreateRequestDto request) {
        String canonical = String.join("|",
                normalize(request.getTitle()),
                normalize(request.getDescription()),
                normalize(request.getContent()),
                normalize(request.getCategory()),
                normalize(request.getSubCategory()),
                normalize(String.valueOf(request.getStartDate())),
                normalize(String.valueOf(request.getEndDate())),
                normalize(String.valueOf(request.getMaxParticipants())),
                normalize(request.getGithubUrl()),
                normalize(request.getDemoUrl()),
                normalize(request.getThumbnailUrl()),
                normalizeAttachments(request.getAttachments())
        );
        return sha256Hex(canonical);
    }

    public String hashUpdate(ProjectUpdateRequestDto request) {
        String canonical = String.join("|",
                normalize(String.valueOf(request.getProjectId())),
                normalize(request.getTitle()),
                normalize(request.getDescription()),
                normalize(request.getContent()),
                normalize(request.getCategory()),
                normalize(request.getSubCategory()),
                normalize(String.valueOf(request.getMaxParticipants())),
                normalize(request.getGithubUrl()),
                normalize(request.getDemoUrl()),
                normalize(request.getThumbnailUrl()),
                normalizeAttachments(request.getAttachments())
        );
        return sha256Hex(canonical);
    }

    public String hashEnd(ProjectEndRequestDto request) {
        String attachmentUrl = request.getAttachment() != null ? request.getAttachment().getAttachedUrl() : "";
        String canonical = String.join("|",
                normalize(String.valueOf(request.getProjectId())),
                normalizeUrl(attachmentUrl)
        );
        return sha256Hex(canonical);
    }

    private String normalizeAttachments(List<ProjectAttachedCreateRequestDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "[]";
        }

        List<String> normalized = new ArrayList<>();
        for (ProjectAttachedCreateRequestDto attachment : attachments) {
            normalized.add(String.join(":",
                    normalize(attachment.getName()),
                    normalize(String.valueOf(attachment.getType())),
                    normalize(String.valueOf(attachment.getSize())),
                    normalizeUrl(attachment.getAttachedUrl())
            ));
        }

        normalized.sort(Comparator.naturalOrder());
        return String.join(",", normalized);
    }

    private String normalize(String value) {
        if (value == null || "null".equals(value)) {
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
