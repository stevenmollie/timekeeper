package be.sbs.timekeeper.application.service;


import be.sbs.timekeeper.application.beans.Project;
import be.sbs.timekeeper.application.enums.ProjectStatus;
import be.sbs.timekeeper.application.exception.BadRequestException;
import be.sbs.timekeeper.application.exception.ProjectNotFoundException;
import be.sbs.timekeeper.application.repository.ProjectRepository;
import be.sbs.timekeeper.application.repository.ProjectRepositoryCustom;
import be.sbs.timekeeper.application.valueobjects.PatchOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectRepositoryCustom projectRepositoryCustom;

    @InjectMocks
    private ProjectService projectService;

    @Captor
    private ArgumentCaptor<Project> projectArgumentCaptor;

    @Captor
    private ArgumentCaptor<PatchOperation> patchOperationArgumentCaptor;



    private static final String PROJECT_ID = "123456abc";

    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Adding Project tests")
    class AddProjectTests {

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Basic adding tests with allowed values")
        class BasicTests {

            @ParameterizedTest
            @MethodSource("allowedParametersValues")
            void test_addProject_withAllowedValues(String projectId, String name, String description, LocalDate deadLine, ProjectStatus status) {
                Project project = new Project(projectId, name, description, deadLine, status);
                projectService.addProject(project);
                verify(projectRepository).insert(projectArgumentCaptor.capture());
                Project result = projectArgumentCaptor.getValue();
                reset(projectRepository);
                assertThat(project).isEqualToComparingFieldByField(result);
            }

            private Stream<Arguments> allowedParametersValues() {
                return Stream.of(
                        Arguments.of(null, "project", "Hello", null, null),
                        Arguments.of(null, "project", "", null, ProjectStatus.EMPTY),
                        Arguments.of(null, "project", "", LocalDate.now(), ProjectStatus.EMPTY),
                        Arguments.of(null, "project", "Hello", LocalDate.now(), ProjectStatus.EMPTY));
            }
        }

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Adding tests that throw Exceptions")
        class ExceptionsTests {

            @ParameterizedTest
            @MethodSource("notAllowedParametersValues")
            void test_addProject_withNotAllowedValues(String projectId, String name, String description, LocalDate deadLine, ProjectStatus status) {
                assertThrows(BadRequestException.class,
                        () -> projectService.addProject(new Project(projectId, name, description, deadLine, status)));
            }

            private Stream<Arguments> notAllowedParametersValues() {
                return Stream.of(
                        Arguments.of(PROJECT_ID, "project", "Description", LocalDate.now(), ProjectStatus.EMPTY),
                        Arguments.of(null, "", "Description", null, ProjectStatus.READY_TO_START),
                        Arguments.of(null, "", "Description", LocalDate.now(), ProjectStatus.READY_TO_START),
                        Arguments.of(null, "", "Description", LocalDate.now(), ProjectStatus.CANCELED),
                        Arguments.of(null, "", "Description", LocalDate.now(), ProjectStatus.DONE),
                        Arguments.of(null, "", "Description", LocalDate.now(), ProjectStatus.IN_PROGRESS),
                        Arguments.of(null, "", "Description", LocalDate.now(), null),
                        Arguments.of(null, null, "Description", LocalDate.now(), ProjectStatus.IN_PROGRESS));
            }
        }

    }

    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Putting Project tests")
    class UpdateProjectTests {

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Basic putting tests with allowed values")
        class BasicTests {

            @ParameterizedTest
            @MethodSource("allowedParametersValues")
            void test_withAllowedValues(String projectId, String name, String description, LocalDate deadLine, ProjectStatus status) {
                Project project = new Project(projectId, name, description, deadLine, status);

                when(projectRepository.findById(projectId)).thenReturn(Optional.of(new Project(null, null, null, null, null)));

                projectService.updateProject(project);

                verify(projectRepository).save(projectArgumentCaptor.capture());
                Project result = projectArgumentCaptor.getValue();
                reset(projectRepository);
                assertThat(project).isEqualToComparingFieldByField(result);
            }

            private Stream<Arguments> allowedParametersValues() {
                return Stream.of(
                        Arguments.of(PROJECT_ID, "project", "Hello", LocalDate.now(), ProjectStatus.EMPTY),
                        Arguments.of(PROJECT_ID, "project", "Hello", LocalDate.now(), ProjectStatus.IN_PROGRESS),
                        Arguments.of(PROJECT_ID, "project", "Hello", LocalDate.now(), ProjectStatus.DONE),
                        Arguments.of(PROJECT_ID, "project", "Hello", LocalDate.now(), ProjectStatus.CANCELED),
                        Arguments.of(PROJECT_ID, "project", "Hello", LocalDate.now(), ProjectStatus.READY_TO_START),
                        Arguments.of(PROJECT_ID, "project", "", LocalDate.now(), ProjectStatus.EMPTY));
            }
        }

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Putting tests that throw Exceptions")
        class ExceptionsTests {

            @ParameterizedTest
            @MethodSource("notAllowedParametersValues")
            void test_withNotAllowedValues(String projectId, String name, String description, LocalDate deadLine, ProjectStatus status) {
                assertThrows(BadRequestException.class,
                        () -> projectService.updateProject(new Project(projectId, name, description, deadLine, status)));
            }

            private Stream<Arguments> notAllowedParametersValues() {
                return Stream.of(
                        Arguments.of(PROJECT_ID, "", "Description", LocalDate.now(), ProjectStatus.EMPTY),
                        Arguments.of(PROJECT_ID, null, "Description", LocalDate.now(), ProjectStatus.EMPTY),
                        Arguments.of(PROJECT_ID, "project", null, LocalDate.now(), ProjectStatus.EMPTY),
                        Arguments.of(PROJECT_ID, "project", "Description", null, ProjectStatus.EMPTY),
                        Arguments.of(PROJECT_ID, "project", "Description", null, null),
                        Arguments.of(null, "project", "Description", LocalDate.now(), ProjectStatus.EMPTY));
            }

            @Test
            void test_ProjectNotFound() {
                when(projectRepository.findById(PROJECT_ID))
                        .thenReturn(Optional.empty());
                assertThrows(ProjectNotFoundException.class,
                        () -> projectService.updateProject(new Project(PROJECT_ID, "project", "Hello", LocalDate.now(), ProjectStatus.EMPTY)));
            }
        }

    }

    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Patch Project tests")
    class PatchProjectTests {

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Basic patching tests with allowed values")
        class BasicTests {

            @ParameterizedTest
            @MethodSource("allowedParametersValues")
            void test_withAllowedValues(String op, String path, String value) {
                PatchOperation operation = new PatchOperation(op, path, value);

                when(projectRepository.findById(PROJECT_ID))
                        .thenReturn(Optional.of(new Project(null, null, null, null, ProjectStatus.READY_TO_START)));
                projectService.applyPatch(PROJECT_ID, operation);

                verify(projectRepositoryCustom).saveOperation(any(String.class), patchOperationArgumentCaptor.capture());
                assertThat(patchOperationArgumentCaptor.getValue()).isEqualToComparingFieldByField(operation);

                reset(projectRepositoryCustom);
            }

            private Stream<Arguments> allowedParametersValues() {
                return Stream.of(
                        Arguments.of("replace", "/deadLine", "1980-08-22"),
                        Arguments.of("replace", "/description", ""),
                        Arguments.of("replace", "/description", "jhbc sdkjhcbzieu"),
                        Arguments.of("replace", "/status", ProjectStatus.EMPTY.name()),
                        Arguments.of("replace", "/status", ProjectStatus.DONE.name()),
                        Arguments.of("replace", "/status", ProjectStatus.READY_TO_START.name()),
                        Arguments.of("replace", "/status", ProjectStatus.CANCELED.name()),
                        Arguments.of("replace", "/status", ProjectStatus.IN_PROGRESS.name()),
                        Arguments.of("replace", "/name", "abcde")
                );
            }
        }

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Patching tests that throw Exceptions")
        class ExceptionsTests {

            @ParameterizedTest
            @MethodSource("notAllowedParametersValues")
            void test_withNotAllowedValues(String op, String path, String value, ProjectStatus statusExistingProject) {
                when(projectRepository.findById(PROJECT_ID))
                        .thenReturn(Optional.of(new Project(null, null, null, null, statusExistingProject)));
                assertThrows(BadRequestException.class,
                        () -> projectService.applyPatch(PROJECT_ID, new PatchOperation(op, path, value)));
            }

            private Stream<Arguments> notAllowedParametersValues() {
                return Stream.of(
                        Arguments.of("replace", "/deadLine", "2019-12-21", ProjectStatus.DONE),
                        Arguments.of("replace", "/name", "2019-12-21", ProjectStatus.DONE),
                        Arguments.of("replace", "/description", "2019-12-21", ProjectStatus.DONE),
                        Arguments.of("replace", "/id", "2019-12-21", ProjectStatus.IN_PROGRESS),
                        Arguments.of("add", "/id", "2019-12-21", ProjectStatus.IN_PROGRESS),
                        Arguments.of("replace", "/deadLine", "21-12-2019", ProjectStatus.IN_PROGRESS)
                );
            }

            @Test
            void test_ProjectNotFound() {
                when(projectRepository.findById(PROJECT_ID))
                        .thenReturn(Optional.empty());
                assertThrows(ProjectNotFoundException.class,
                        () -> projectService.updateProject(new Project(PROJECT_ID, "project", "Hello", LocalDate.now(), ProjectStatus.EMPTY)));
            }
        }

    }
}