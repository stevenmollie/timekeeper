package be.sbs.timekeeper.application.service;

import be.sbs.timekeeper.application.beans.Task;
import be.sbs.timekeeper.application.beans.User;
import be.sbs.timekeeper.application.exception.ActivationTokenNotCorrectException;
import be.sbs.timekeeper.application.exception.BadMailFormatException;
import be.sbs.timekeeper.application.exception.ResetTokenExpiredException;
import be.sbs.timekeeper.application.exception.UserAlreadyActivatedException;
import be.sbs.timekeeper.application.exception.UserAlreadyExistsException;
import be.sbs.timekeeper.application.exception.UserNotActiveException;
import be.sbs.timekeeper.application.exception.UserNotFoundException;
import be.sbs.timekeeper.application.repository.UserRepository;
import be.sbs.timekeeper.application.valueobjects.FieldValidator;
import be.sbs.timekeeper.application.valueobjects.PatchOperation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskService taskService;
    private final static Duration MAX_RESET_TOKEN_LIFE = Duration.ofMinutes(10);
    
    @Autowired
    private MailService mailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, MailService mailService, TaskService taskService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.taskService = taskService;
    }

    public User getById(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    public User getByToken(String token) {
    	return userRepository.findFirstByToken(token).orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    public User login(User inputUser) {
        User outputUser = userRepository.findFirstByName(inputUser.getName())
                          .orElseThrow(() -> new UserNotFoundException("User not found"));

        if(!passwordIsCorrect(inputUser.getPassword(), outputUser.getPassword())){
            throw new UserNotFoundException("Incorrect password");
        }
        
        if(!outputUser.getActive()) {
        	throw new UserNotActiveException("User not active");
        }

        outputUser.setToken(createToken());

        return userRepository.save(outputUser);
    }
    
    public void activate(User inputUser) {
    	User outputUser = userRepository.findFirstByName(inputUser.getName())
    						.orElseThrow(() -> new UserNotFoundException("User not found"));
    	
    	if(!outputUser.getActivationToken().equals(inputUser.getActivationToken())) {
    		throw new ActivationTokenNotCorrectException("Token not correct");
    	}
    	
    	if(outputUser.getActive() == true) {
    		throw new UserAlreadyActivatedException("User already activated");
    	}
    	
    	outputUser.setActive(true);
    	
    	userRepository.save(outputUser);
    }
    
    public User register(User inputUser) {
    	if(!isEmailAddress(inputUser.getEmail())) {
    		throw new BadMailFormatException("Invalid email");
    	}
    	checkIfUserExists(inputUser);
    	
    	inputUser.setPassword(passwordEncoder.encode(inputUser.getPassword()));
    	inputUser.setActive(false);
    	inputUser.setActivationToken(createToken());
    	User outputUser = userRepository.save(inputUser);
    	
    	mailService.sendActivationMail(outputUser.getEmail(), outputUser.getActivationToken(), outputUser.getName());
    	return outputUser;
    }
    

    
    public void sendResetPasswordMail(User inputUser) {
    	if(!isEmailAddress(inputUser.getEmail())) {
    		throw new BadMailFormatException("Invalid email");
    	}
    	
    	User outputUser = userRepository.findFirstByEmail(inputUser.getEmail())
				.orElseThrow(() -> new UserNotFoundException("User not found"));
    	
    	outputUser.setResetPasswordToken(createToken());
    	outputUser.setResetTime(LocalDateTime.now());
    	outputUser = userRepository.save(outputUser);
    	
    	mailService.sendResetPasswordMail(outputUser);
    }
    
    public void resetPassword(User inputUser) {
    	User outputUser = userRepository.findFirstByNameAndResetPasswordToken(inputUser.getName(), inputUser.getResetPasswordToken())
				.orElseThrow(() -> new UserNotFoundException("User not found"));
    	
    	if(Duration.between(LocalDateTime.now(), outputUser.getResetTime()).compareTo(MAX_RESET_TOKEN_LIFE) > 0) {
    		throw new ResetTokenExpiredException("Reset token expired");
    	}
    	
    	outputUser.setPassword(passwordEncoder.encode(inputUser.getPassword()));
    	outputUser.setResetPasswordToken(null);
    	outputUser.setResetTime(null);
    	
    	userRepository.save(outputUser);
    }

	private void checkIfUserExists(User inputUser) {
		userRepository.findFirstByName(inputUser.getName())
    		.ifPresent(user -> throwUserExistsException());
		userRepository.findFirstByEmail(inputUser.getEmail())
			.ifPresent(user -> throwUserExistsException());
	}

	private void throwUserExistsException() {
		throw new UserAlreadyExistsException("User already exists");
	}
    
    
    private boolean passwordIsCorrect(String inputPassword, String outputPassword) {
        return passwordEncoder.matches(inputPassword, outputPassword);
    }

    public boolean userAuthenticated(String token){

        return userRepository.findFirstByToken(token).isPresent();
    }

    private String createToken() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 128;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }
    
    private boolean isEmailAddress(String mail) {
    	Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
    	Matcher matcher = pattern.matcher(mail);
    	
    	return matcher.matches();
    }

    public void saveUser(User user){
        userRepository.save(user);
    }

    public void applyPatch(PatchOperation patch, String token) {
        User user = getByToken(token);
        FieldValidator.validatePATCHUser(patch);
        Task task = taskService.getById(patch.getValue());
        user.setSelectedTask(task.getId());
        user.setSelectedProject(task.getProjectId());
        userRepository.save(user);
    }
}
