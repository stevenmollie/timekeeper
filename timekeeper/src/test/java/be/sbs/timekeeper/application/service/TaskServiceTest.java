package be.sbs.timekeeper.application.service;


import be.sbs.timekeeper.application.beans.Project;
import be.sbs.timekeeper.application.beans.Task;
import be.sbs.timekeeper.application.enums.Priority;
import be.sbs.timekeeper.application.enums.ProjectStatus;
import be.sbs.timekeeper.application.enums.TaskStatus;
import be.sbs.timekeeper.application.exception.BadRequestException;
import be.sbs.timekeeper.application.exception.TaskNotFoundException;
import be.sbs.timekeeper.application.repository.TaskRepository;
import be.sbs.timekeeper.application.repository.TaskRepositoryCustom;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    private static final String PROJECT_ID = "123456abc";
    private static final String TASK_ID = "123456abc";
    private static final String DATE_TIME_STRING = "2018-07-24T11:18:58";
    private static final String DATE_TIME_STRING_SHORT = "2018-07-24";

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskRepositoryCustom taskRepositoryCustom;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private TaskService taskService;

    @Captor
    private ArgumentCaptor<Task> taskArgumentCaptor;

    @Captor
    private ArgumentCaptor<PatchOperation> patchOperationArgumentCaptor;


    @Nested
    @DisplayName("POST task tests")
    class AddTaskTests {

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Basic adding tests with allowed values")
        class BasicTests {

            @ParameterizedTest
            @MethodSource("allowedParametersValues")
            void test_withAllowedValues(String id, String name, String description, String projectId, LocalDateTime currentTime, Priority priority, TaskStatus status) {
                Task task = new Task(id, name, description, projectId, currentTime, priority, status);

                when(projectService.getById(PROJECT_ID))
                        .thenReturn(new Project(null, null, null, null, ProjectStatus.READY_TO_START));
                taskService.addTask(task);

                assertThat(task).isEqualToComparingFieldByField(interceptInsertInDB());
            }

            private Stream<Arguments> allowedParametersValues() {
                return Stream.of(
                        Arguments.of(null, "name", "desc", PROJECT_ID, null, Priority.MEDIUM, TaskStatus.READY_TO_START),
                        Arguments.of(null, "name", "", PROJECT_ID, null, Priority.MEDIUM, TaskStatus.READY_TO_START),
                        Arguments.of(null, "name", "", PROJECT_ID, LocalDateTime.now(), Priority.MEDIUM, TaskStatus.READY_TO_START),
                        Arguments.of(null, "name", "", PROJECT_ID, LocalDateTime.now(), Priority.MEDIUM, null),
                        Arguments.of(null, "name", "", PROJECT_ID, LocalDateTime.now(), null, TaskStatus.READY_TO_START),
                        Arguments.of(null, "name", "", PROJECT_ID, LocalDateTime.now(), null, null),
                        Arguments.of(null, "name", "desc", PROJECT_ID, LocalDateTime.now(), Priority.MEDIUM, TaskStatus.READY_TO_START));
            }

            @Test
            public void testDefaultPriority() {
                Task task = new Task(null, "name", "", PROJECT_ID, LocalDateTime.now(), null, TaskStatus.READY_TO_START);

                when(projectService.getById(PROJECT_ID))
                        .thenReturn(new Project(null, null, null, null, ProjectStatus.READY_TO_START));
                taskService.addTask(task);

                assertThat(interceptInsertInDB().getPriority()).isEqualTo(Priority.MEDIUM);
            }

            @Test
            public void testDefaultStatus() {
                Task task = new Task(null, "name", "", PROJECT_ID, LocalDateTime.now(), Priority.MEDIUM, TaskStatus.READY_TO_START);

                when(projectService.getById(PROJECT_ID))
                        .thenReturn(new Project(null, null, null, null, ProjectStatus.READY_TO_START));
                taskService.addTask(task);

                assertThat(interceptInsertInDB().getStatus()).isEqualTo(TaskStatus.READY_TO_START);
            }

            private Task interceptInsertInDB() {
                verify(taskRepository).insert(taskArgumentCaptor.capture());
                Task result = taskArgumentCaptor.getValue();
                reset(taskRepository);
                return result;
            }

        }

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Adding tests that throw Exceptions")
        class ExceptionsTests {

            @ParameterizedTest
            @MethodSource("notAllowedParametersValues")
            void test_withNotAllowedValues(String id, String name, String description, String projectId, LocalDateTime currentTime, Priority priority, TaskStatus status) {
                assertThrows(BadRequestException.class,
                        () -> taskService.addTask(new Task(id, name, description, projectId, currentTime, priority, status)));
            }

            private Stream<Arguments> notAllowedParametersValues() {
                return Stream.of(
                        Arguments.of(TASK_ID, "name", "desc", PROJECT_ID, LocalDateTime.now(), Priority.MEDIUM, TaskStatus.READY_TO_START),
                        Arguments.of(null, null, "desc", PROJECT_ID, LocalDateTime.now(), Priority.MEDIUM, TaskStatus.READY_TO_START),
                        Arguments.of(null, "", "desc", PROJECT_ID, LocalDateTime.now(), Priority.MEDIUM, TaskStatus.READY_TO_START),
                        Arguments.of(null, "", "desc", PROJECT_ID, LocalDateTime.now(), null, TaskStatus.READY_TO_START),
                        Arguments.of(null, "name", "desc", PROJECT_ID, null, Priority.MEDIUM, TaskStatus.DONE),
                        Arguments.of(null, "name", "desc", PROJECT_ID, null, Priority.MEDIUM, TaskStatus.IN_PROGRESS),
                        Arguments.of(null, "name", "desc", PROJECT_ID, null, Priority.MEDIUM, TaskStatus.CANCELED),
                        Arguments.of(null, "", "desc", PROJECT_ID, LocalDateTime.now(), null, null),
                        Arguments.of(null, "name", "desc", null, LocalDateTime.now(), Priority.MEDIUM, TaskStatus.READY_TO_START));
            }
        }
    }

    @Nested
    @DisplayName("PATCH task tests")
    class PatchTaskTests {

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Basic patching tests with allowed values")
        class BasicTests {

            @ParameterizedTest
            @MethodSource("allowedParametersValues")
            void test_withAllowedValues(String id, String op, String path, String value) {
                PatchOperation patchOperation = new PatchOperation(op, path, value);
                injectFindById(id);

                taskService.applyPatch(id, patchOperation);

                assertThat(patchOperation).isEqualToComparingFieldByField(interceptSaveToDB(id));
            }

            private Stream<Arguments> allowedParametersValues() {
                return Stream.of(
                        Arguments.of(TASK_ID, "replace", "/currentTime", DATE_TIME_STRING),

                        Arguments.of(TASK_ID, "replace", "/priority", Priority.VERY_LOW.name()),
                        Arguments.of(TASK_ID, "replace", "/priority", Priority.LOW.name()),
                        Arguments.of(TASK_ID, "replace", "/priority", Priority.MEDIUM.name()),
                        Arguments.of(TASK_ID, "replace", "/priority", Priority.HIGH.name()),
                        Arguments.of(TASK_ID, "replace", "/priority", Priority.VERY_HIGH.name()),

                        Arguments.of(TASK_ID, "replace", "/status", TaskStatus.READY_TO_START.name()),
                        Arguments.of(TASK_ID, "replace", "/status", TaskStatus.IN_PROGRESS.name()),
                        Arguments.of(TASK_ID, "replace", "/status", TaskStatus.CANCELED.name()),
                        Arguments.of(TASK_ID, "replace", "/status", TaskStatus.DONE.name()));
            }

            private PatchOperation interceptSaveToDB(String id) {
                verify(taskRepositoryCustom).saveOperation(eq(id), patchOperationArgumentCaptor.capture());

                PatchOperation result = patchOperationArgumentCaptor.getValue();
                reset(taskRepositoryCustom);
                reset(taskRepository);
                return result;
            }
        }

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Patching tests that throw Exceptions")
        class ExceptionsTests {

            @ParameterizedTest
            @MethodSource("notAllowedParametersValues")
            void test_withNotAllowedValues(String id, String op, String path, String value) {
                assertThrows(BadRequestException.class,
                        () -> taskService.applyPatch(id, new PatchOperation(op, path, value)));
            }

            private Stream<Arguments> notAllowedParametersValues() {
                return Stream.of(
                        Arguments.of(TASK_ID, "add", "/currentTime", DATE_TIME_STRING),
                        Arguments.of(TASK_ID, "replace", "/currentTime", null),
                        Arguments.of(TASK_ID, "replace", "/name", DATE_TIME_STRING));
            }

            @Test
            void test_noTaskId() {
                assertThrows(TaskNotFoundException.class,
                        () -> taskService.applyPatch(null, new PatchOperation("replace", "/currentTime", DATE_TIME_STRING)));
            }
        }
    }

    @Nested
    @DisplayName("PUT task tests")
    class UpdateTaskTests {

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Basic putting tests with allowed values")
        class BasicTests {

            @ParameterizedTest
            @MethodSource("allowedParametersValues")
            void test_withAllowedValues(String id, String name, String description, String projectId, LocalDateTime currentTime, TaskStatus status) {
                Task task = new Task(id, name, description, projectId, currentTime, Priority.MEDIUM, status);
                injectFindById(id);

                taskService.updateTask(task);
                verify(taskRepository).save(taskArgumentCaptor.capture());
                Task result = taskArgumentCaptor.getValue();
                reset(taskRepository);
                assertThat(task).isEqualToComparingFieldByField(result);
            }

            private Stream<Arguments> allowedParametersValues() {
                return Stream.of(
                        Arguments.of(TASK_ID, "name", "desc", PROJECT_ID, LocalDateTime.now(), TaskStatus.READY_TO_START),
                        Arguments.of(TASK_ID, "name", "", PROJECT_ID, LocalDateTime.now(), TaskStatus.READY_TO_START));
            }
        }

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Putting tests that throw Exceptions")
        class ExceptionsTests {

            @ParameterizedTest
            @MethodSource("notAllowedParametersValues")
            void test_withNotAllowedValues(String id, String name, String description, String projectId, LocalDateTime currentTime, TaskStatus status) {
                assertThrows(BadRequestException.class,
                        () -> taskService.updateTask(new Task(id, name, description, projectId, currentTime, Priority.MEDIUM, status)));
            }

            private Stream<Arguments> notAllowedParametersValues() {
                return Stream.of(
                        Arguments.of(null, "name", "desc", PROJECT_ID, LocalDateTime.now(), TaskStatus.READY_TO_START),
                        Arguments.of(TASK_ID, null, "desc", PROJECT_ID, LocalDateTime.now(), TaskStatus.READY_TO_START),
                        Arguments.of(TASK_ID, null, "desc", null, LocalDateTime.now(), TaskStatus.READY_TO_START),
                        Arguments.of(TASK_ID, null, "desc", PROJECT_ID, null, TaskStatus.READY_TO_START),
                        Arguments.of(TASK_ID, null, "desc", PROJECT_ID, null, null),
                        Arguments.of(TASK_ID, "name", null, PROJECT_ID, LocalDateTime.now(), TaskStatus.READY_TO_START));
            }


            @Test
            void test_TaskNotFound() {
                when(taskRepository.findById(TASK_ID))
                        .thenReturn(Optional.empty());
                assertThrows(TaskNotFoundException.class,
                        () -> taskService.updateTask(new Task(TASK_ID, "name", "desc", PROJECT_ID, LocalDateTime.now(), Priority.MEDIUM, TaskStatus.DONE)));
            }
        }


    }

    @Nested
    @DisplayName("DELETE task tests")
    class DeleteTaskTests {

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Deleting tests that throw Exceptions")
        class ExceptionsTests {
            @Test
            void test_TaskNotFound() {
                when(taskRepository.findById(TASK_ID))
                        .thenReturn(Optional.empty());
                assertThrows(TaskNotFoundException.class,
                        () -> taskService.deleteTask(TASK_ID));
            }
        }


    }

    private void injectFindById(String id) {
        when(taskRepository.findById(id))
                .thenReturn(Optional.of(new Task(TASK_ID, "testing find by id injection", "", PROJECT_ID, null, Priority.MEDIUM, TaskStatus.IN_PROGRESS)));
    }
}