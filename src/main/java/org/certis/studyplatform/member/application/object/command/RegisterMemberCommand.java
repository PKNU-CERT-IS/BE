package org.certis.studyplatform.member.application.object.command;

import java.time.OffsetDateTime;


// 순수 회원가입을 위한 command 객체
// Role 이 없는이유는 PENDING 고정 값이기 때문 -> CreateMemberCommand의 팩토리 생성자 메소드로 합쳐도 되지만 ddd 관점에서 새로운 객체를 만듬
public record RegisterMemberCommand(
        String name,
        String accountNumber,
        String password,
        String studentNumber,
        String grade,
        String major,
        OffsetDateTime birthday,
        String gender
) {
}
