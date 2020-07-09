package in.projecteka.gateway.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeasy.random.EasyRandom;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

public class TestBuilders {
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.configure(WRITE_DATES_AS_TIMESTAMPS, false);

	private static final EasyRandom easyRandom = new EasyRandom();

	public static String string() {
		return easyRandom.nextObject(String.class);
	}
}
