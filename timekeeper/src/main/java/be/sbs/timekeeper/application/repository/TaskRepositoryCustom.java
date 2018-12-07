package be.sbs.timekeeper.application.repository;

import be.sbs.timekeeper.application.beans.Task;
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
        System.out.println(query.toString());
        System.out.println("update = " + update.toString());
        UpdateResult updateResult = mongoOperations.updateFirst(query, update, Task.class);
        System.out.println("updateResult.toString() = " + updateResult.toString());
        if (!updateResult.wasAcknowledged()) {
            throw new MongoException("Could not add operation " + operation + " to task " + taskId);
        }
    }
}
