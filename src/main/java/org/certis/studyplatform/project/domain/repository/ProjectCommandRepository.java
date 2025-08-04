//package org.certis.studyplatform.project.domain.repository;
//
//import org.certis.studyplatform.member.domain.model.vo.MemberIdVo;
//import org.certis.studyplatform.project.domain.model.Project;
//import org.certis.studyplatform.project.domain.model.vo.ProjectIdVo;
//
//import java.util.Optional;
//
///**
// * Project Command Repository Interface
// *
// * CQRS Command 측면의 Repository (Write 작업)
// * Domain Layer의 인터페이스
// * Infrastructure Layer에서 구현
// */
//public interface ProjectCommandRepository {
//
//    /**
//     * 프로젝트 저장 (생성/수정)
//     *
//     * @param project 저장할 프로젝트 도메인 객체
//     * @return 저장된 프로젝트 도메인 객체
//     */
//    Project save(Project project);
//
//    /**
//     * 프로젝트 ID로 조회 (Command 용)
//     *
//     * @param projectId 조회할 프로젝트 ID
//     * @return 프로젝트 도메인 객체
//     */
//    Optional<Project> findById(ProjectIdVo projectId);
//
//    /**
//     * 프로젝트 삭제
//     *
//     * @param projectId 삭제할 프로젝트 ID
//     */
//    void deleteById(ProjectIdVo projectId);
//
//    /**
//     * 프로젝트 존재 여부 확인
//     *
//     * @param projectId 확인할 프로젝트 ID
//     * @return 존재 여부
//     */
//    boolean existsById(ProjectIdVo projectId);
//
//    /**
//     * 특정 회원이 생성한 프로젝트 개수 조회
//     *
//     * @param creatorId 생성자 ID
//     * @return 프로젝트 개수
//     */
//    long countByCreatorId(MemberIdVo creatorId);
//
//    /**
//     * 프로젝트 제목 중복 확인 (같은 생성자 내에서)
//     *
//     * @param creatorId 생성자 ID
//     * @param title 제목
//     * @return 중복 여부
//     */
//    boolean existsByCreatorIdAndTitle(MemberIdVo creatorId, String title);
//}