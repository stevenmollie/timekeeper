package be.sbs.timekeeper.application.service;

import be.sbs.timekeeper.application.beans.Task;
import be.sbs.timekeeper.application.enums.TaskStatus;
import be.sbs.timekeeper.application.service.TaskService;
import be.sbs.timekeeper.application.beans.Session;
import be.sbs.timekeeper.application.exception.BadRequestException;
//import be.sbs.timekeeper.application.beans.User;
import be.sbs.timekeeper.application.exception.SessionNotFoundException;
import be.sbs.timekeeper.application.repository.SessionRepository;
import be.sbs.timekeeper.application.repository.SessionRepositoryCustom;
import be.sbs.timekeeper.application.valueobjects.FieldConverter;
import be.sbs.timekeeper.application.valueobjects.FieldValidator;
import be.sbs.timekeeper.application.valueobjects.PatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionRepositoryCustom sessionRepositoryCustom;
    
    @Autowired
    private TaskService taskService;

    public List<Session> getAll() {
        return sessionRepository.findAll();
    }
    
    public List<Session> getAllSessionsFromTask(Task task) {
        return sessionRepositoryCustom.findSessionsByTaskId(task.getId());
    }

    //uncomment me when User bean is created!
    /*
    public List<Session> getAllSessionsFromUser(User user) {
    	return sessionRepositoryCustom.findSessionsByUserId(user.getId());
    }
    */
    
    public Session getById(String sessionId) {
        return sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session not found"));
    }

    public void addSession(Session session) {
    	Task task = taskService.getById(session.getTaskId());
    	FieldValidator.validatePOSTSession(session, task.getStatus());
        FieldConverter.setDefaultSessionFields(session);
        Session newSession = sessionRepository.insert(session);
        if(newSession != null) {
        	//check if TaskStatus needs to be changed
        	if(task.getStatus() == TaskStatus.READY_TO_START) {
        		//set taskStatus to IN_PROGRESS
        		taskService.setTaskStatus(newSession.getTaskId(), TaskStatus.IN_PROGRESS);
        	}
        }
    }

    //TODO: upon entering of session endTime, do stuff to end that session
    
    public void applyPatch(String sessionId, PatchOperation patchOperation) {
        FieldValidator.validatePATCHSession(patchOperation);
        FieldConverter.convertSessionFields(patchOperation);
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Cannot patch session: " + sessionId + ". the session doesn't exist!"));
        sessionRepositoryCustom.saveOperation(sessionId, patchOperation);
    }

    public void updateSession(Session session) {
        FieldValidator.validatePUTSession(session);
        sessionRepository.findById(session.getId())
                .orElseThrow(() -> new SessionNotFoundException("Cannot update session: " + session.getId() + ". the session doesn't exist!"));
        sessionRepository.save(session);
    }

    public void deleteSession(String sessionId) {
        sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session : " + sessionId + " doesn't exist!"));
        sessionRepository.deleteById(sessionId);
    }
    
    public void deleteSessionsFromTask(String taskId) {
    	sessionRepositoryCustom.deleteSessionsFromTaskId(taskId);
    }
    
    public void deleteSessionsFromUser(String userId) {
    	sessionRepositoryCustom.deleteSessionsFromUserId(userId);
    }
}