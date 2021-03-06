package be.sbs.timekeeper.application.repository;

import be.sbs.timekeeper.application.beans.Task;
import be.sbs.timekeeper.application.enums.TaskStatus;
import be.sbs.timekeeper.application.valueobjects.PatchOperation;
import com.mongodb.MongoException;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskRepositoryCustom {
    private MongoOperations mongoOperations;

    public TaskRepositoryCustom(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public List<Task> findTasksByProjectId(String projectId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("projectId").is(projectId));
        return mongoOperations.find(query, Task.class);
    }

    public void saveOperation(String taskId, PatchOperation operation) {
        Query query = Query.query(Criteria.where("id").is(taskId));
        Update update = new Update().set(operation.getPath().substring(1), operation.getValue());
        UpdateResult updateResult = mongoOperations.updateFirst(query, update, Task.class);
        if (!updateResult.wasAcknowledged()) {
            throw new MongoException("Could not add operation " + operation + " to task " + taskId);
        }
    }
    
    public void deleteTasksFromProject(String projectId) {
    	Query query = new Query();
    	query.addCriteria(Criteria.where("projectId").is(projectId));
    	mongoOperations.findAllAndRemove(query, Task.class);
    }

	public void updateTaskStatus(String taskId, TaskStatus taskStatus) {
		Query query = Query.query(Criteria.where("id").is(taskId));
		Update update = new Update().set("status", taskStatus.name());
		UpdateResult updateResult = mongoOperations.updateFirst(query, update, Task.class);
		if (!updateResult.wasAcknowledged()) {
			throw new MongoException("Could not update task status to " + taskStatus.name() + " in task " + taskId);
		}
	}
}
