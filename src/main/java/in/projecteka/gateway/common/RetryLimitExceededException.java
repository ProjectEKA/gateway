package in.projecteka.gateway.common;

public class RetryLimitExceededException extends Throwable {
    public RetryLimitExceededException(String message){
        super(message);
    }
}
