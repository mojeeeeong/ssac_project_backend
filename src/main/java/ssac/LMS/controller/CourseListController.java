package ssac.LMS.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ssac.LMS.domain.Course;
import ssac.LMS.domain.Enrollment;
import ssac.LMS.dto.CourseResponseDto;
import ssac.LMS.dto.CourseSearchResponseDto;
import ssac.LMS.dto.MyCourseResponseDto;
import ssac.LMS.service.CourseListService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
@Slf4j
public class CourseListController {
    private final CourseListService courseListService;

    @Data
    static class Result<T> {
        private int count;
        private T data;

        public Result(int count, T data) {
            this.count = count;
            this.data = data;
        }
    }
    @GetMapping("/new")
    public ResponseEntity<Result> getCourseByNew() {
        log.info("new");
        List<Course> latestCourses = courseListService.getLatestCourses();

        List<CourseResponseDto> courseResponseDtoStream = latestCourses.stream()
                .map(m -> new CourseResponseDto(m.getTitle(), m.getDescription(), m.getStartedAt(), m.getPrice(),
                        m.getTags(), m.getThumbnailPath()))
                        .collect(Collectors.toList());
        return ResponseEntity.status(HttpServletResponse.SC_OK).body(new Result(courseResponseDtoStream.size(), courseResponseDtoStream));
    }

    @GetMapping("/best")
    public ResponseEntity<Result> getCourseByBest() {

        log.info("best");
        List<Course> bestCourses = courseListService.getCourseByBest();
        List<CourseResponseDto> courseResponseDtoStream = bestCourses.stream()
                .map(m -> new CourseResponseDto(m.getTitle(), m.getDescription(), m.getStartedAt(), m.getPrice(),
                        m.getTags(), m.getThumbnailPath()))
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpServletResponse.SC_OK).body(new Result(courseResponseDtoStream.size(), courseResponseDtoStream));

    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getMyClass(@PathVariable String userId, @AuthenticationPrincipal Jwt jwt) {
        log.info("getUserId={}", userId);
        log.info("jwtUserId={}", jwt.getClaim("cognito:username").toString());

        if (!userId.equals(jwt.getClaim("cognito:username").toString())) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Id가 맞지 않습니다.");
        }

        List<Enrollment> myClass = courseListService.getMyClass(userId);
        List<MyCourseResponseDto> myCourseResponseDto = myClass.stream()
                .map(m -> new MyCourseResponseDto(m.getCourse().getCourseId(), m.getCourse().getTitle(), m.getCourse().getUser().getUserName(), m.getEnrolledAt(), m.getCourse().getThumbnailPath()))
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpServletResponse.SC_OK).body(new Result(myCourseResponseDto.size(), myCourseResponseDto));
    }

    @GetMapping("/search")
    public ResponseEntity<?> getSearchCourse(@RequestParam("keyword") String keyword) {
        List<Course> searchCourse = courseListService.getSearchCourse(keyword);

        // 각 강의 정보를 CourseSearchInfo로 변환하여 리스트에 추가
        List<CourseSearchResponseDto> responseDtoList = searchCourse.stream()
                .map(course -> new CourseSearchResponseDto(course.getTitle(), course.getDescription(), course.getCourseId(), course.getThumbnailPath(),  course.getTags(), course.getUser().getUserName()))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(new Result(responseDtoList.size(), responseDtoList));
    }

}

