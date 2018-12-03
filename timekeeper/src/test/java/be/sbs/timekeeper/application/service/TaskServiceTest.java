package be.sbs.timekeeper.application.service;


import be.sbs.timekeeper.application.beans.Task;
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
class TaskServiceTest {

    private static final String PROJECT_ID = "123456abc";
    private static final String TASK_ID = "123456abc";
    private static final String DATE_TIME_STRING = "2018-07-24T11:18:58";

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskRepositoryCustom taskRepositoryCustom;

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
            void test_withAllowedValues(String id, String name, String description, String projectId, LocalDateTime currentTime) {
                Task task = new Task(id, name, description, projectId, currentTime);
                taskService.addTask(task);
                verify(taskRepository).insert(taskArgumentCaptor.capture());
                Task result = taskArgumentCaptor.getValue();
                reset(taskRepository);
                assertThat(task).isEqualToComparingFieldByField(result);
            }

            private Stream<Arguments> allowedParametersValues() {
                return Stream.of(
                        Arguments.of(null, "name", "desc", PROJECT_ID, null),
                        Arguments.of(null, "name", "", PROJECT_ID, null),
                        Arguments.of(null, "name", "", PROJECT_ID, LocalDateTime.now()),
                        Arguments.of(null, "name", "desc", PROJECT_ID, LocalDateTime.now()));
            }

        }

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Adding tests that throw Exceptions")
        class ExceptionsTests {

            @ParameterizedTest
            @MethodSource("notAllowedParametersValues")
            void test_withNotAllowedValues(String id, String name, String description, String projectId, LocalDateTime currentTime) {
                assertThrows(BadRequestException.class,
                        () -> taskService.addTask(new Task(id, name, description, projectId, currentTime)));
            }

            private Stream<Arguments> notAllowedParametersValues() {
                return Stream.of(
                        Arguments.of(TASK_ID, "name", "desc", PROJECT_ID, LocalDateTime.now()),
                        Arguments.of(null, null, "desc", PROJECT_ID, LocalDateTime.now()),
                        Arguments.of(null, "", "desc", PROJECT_ID, LocalDateTime.now()),
                        Arguments.of(null, "name", "desc", null, LocalDateTime.now()));
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
                when(taskRepository.findById(id))
                        .thenReturn(Optional.of(new Task(null, null, null, null, null)));

                taskService.applyPatch(id, patchOperation);

                verify(taskRepositoryCustom).saveOperation(eq(id), patchOperationArgumentCaptor.capture());

                PatchOperation result = patchOperationArgumentCaptor.getValue();
                reset(taskRepositoryCustom);
                reset(taskRepository);
                assertThat(patchOperation).isEqualToComparingFieldByField(result);
            }

            private Stream<Arguments> allowedParametersValues() {
                return Stream.of(
                        Arguments.of(TASK_ID, "replace", "/currentTime", DATE_TIME_STRING),
                        Arguments.of(TASK_ID, "replace", "/deadLine", DATE_TIME_STRING));
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
            void test_withAllowedValues(String id, String name, String description, String projectId, LocalDateTime currentTime) {
                Task task = new Task(id, name, description, projectId, currentTime);
                taskService.updateTask(task);
                verify(taskRepository).save(taskArgumentCaptor.capture());
                Task result = taskArgumentCaptor.getValue();
                reset(taskRepository);
                assertThat(task).isEqualToComparingFieldByField(result);
            }

            private Stream<Arguments> allowedParametersValues() {
                return Stream.of(
                        Arguments.of(TASK_ID, "name", "desc", PROJECT_ID, LocalDateTime.now()),
                        Arguments.of(TASK_ID, "name", "", PROJECT_ID, LocalDateTime.now()));
            }
        }

        @Nested
        @TestInstance(PER_CLASS)
        @DisplayName("Patching tests that throw Exceptions")
        class ExceptionsTests {

            @ParameterizedTest
            @MethodSource("notAllowedParametersValues")
            void test_withNotAllowedValues(String id, String name, String description, String projectId, LocalDateTime currentTime) {
                assertThrows(BadRequestException.class,
                        () -> taskService.addTask(new Task(id, name, description, projectId, currentTime)));
            }

            private Stream<Arguments> notAllowedParametersValues() {
                return Stream.of(
                        Arguments.of(TASK_ID, "name", "desc", PROJECT_ID, LocalDateTime.now()),
                        Arguments.of(null, null, "desc", PROJECT_ID, LocalDateTime.now()),
                        Arguments.of(null, "", "desc", PROJECT_ID, LocalDateTime.now()),
                        Arguments.of(null, "name", "desc", null, LocalDateTime.now()));
            }


            @Test
            void test_noTaskId() {
                assertThrows(TaskNotFoundException.class,
                        () -> taskService.applyPatch(null, new PatchOperation("replace", "/currentTime", DATE_TIME_STRING)));
            }
        }


    }
}