package zerobase.weather.service;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.error.InvalidDate;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DiaryService {
    @Value("${openweathermap.key}")
    private String apiKey;

    private final DiaryRepository diaryRepository;

    private final DateWeatherRepository dateWeatherRepository;

    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);

    public DiaryService(DiaryRepository diaryRepository, DateWeatherRepository dateWeatherRepository) {
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void saveWeatherDate() {
        logger.info("Saving weather data");
        dateWeatherRepository.save(getWeatherFromApi());
    }

    private DateWeather getWeatherFromApi() {
        // open weather map 에서 날씨 데이터 받아오기
        String weatherData = getWeatherString();

        // 받아온 날씨 json 파싱하기
        Map<String, Object> parsedWeather = parseWeather(weatherData);

        DateWeather dateWeather = new DateWeather();
        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parsedWeather.get("main").toString());
        dateWeather.setIcon(parsedWeather.get("icon").toString());
        dateWeather.setTemperature((double) parsedWeather.get("temp"));
        return dateWeather;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createDiary(LocalDate date, String text) {
        logger.info("Started to create diary");

        // 날씨 데이터 가져오기 (API or DB 에서 가져오기)
        DateWeather dateWeather = getDateWeather(date);

        // 파싱된 데이터 + 일기 값 DB 에 넣기
        Diary nowDiary = new Diary();
        nowDiary.setDateWeather(dateWeather);
        nowDiary.setText(text);
        diaryRepository.save(nowDiary);

        logger.info("End to create diary");
    }

    private DateWeather getDateWeather(LocalDate date) {
        List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);

        if (dateWeatherListFromDB.size() == 0) {
            // API 에서 날씨 정보 가져오기
            return getWeatherFromApi();
        } else {
            return dateWeatherListFromDB.get(0);
        }
    }

    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date) {
//        if (date.isAfter(LocalDate.ofYearDay(3050, 1))) {
//            throw new InvalidDate();
//        }
        return diaryRepository.findAllByDate(date);
    }

    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    public void updateDiary(LocalDate date, String text) {
        Diary nowDiary = diaryRepository.getFirstByDate(date);
        nowDiary.setText(text);
        diaryRepository.save(nowDiary);
    }

    public void deleteDiary(LocalDate date) {
        diaryRepository.deleteAllByDate(date);
    }

    private String getWeatherString() {
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey;

        try {
            // apiUrl 사용하여 URL 객체 생성
            URL url = new URL(apiUrl);

            // url.openConnection() 메서드를 호출해 서버와 연결을 설정하는 객체 생성
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // HTTP 요청의 메서드를 GET 으로 설정
            connection.setRequestMethod("GET");

            // 서버로부터 응답 코드 가져오기 (200: 요청 성공, 404: 리소스 찾을 수 없음, 500: 서버 에러)
            int responseCode = connection.getResponseCode();

            BufferedReader br;
            if (responseCode == 200) {
                // BufferedReader 는 효율적으로 데이터를 읽도록 돕는 래퍼 클래스
                // InputStreamReader 는 바이트 스트림을 문자 스트림으로 변환
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }

            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            // 저장된 전체 응답 데이터를 문자열 형태로 반환
            return response.toString();
        } catch (Exception e) {
            return "Failed to get response";
        }
    }

    /**
     * JSONParser 와 JSONObject 를 사용하는 이유
     * org.json.simple 라이브러리에서 제공되는 이 기능은 비교적 가볍고 단순한 JSON 파싱 기능을 제공하기 때문
     * 더 정교하고 복잡한 JSON 데이터를 다룰 때는 Gson, Jackson 등등 고려해보기
     */
    private Map<String, Object> parseWeather(String jsonString) {
        // JSON 데이터를 파싱(문자열 -> JSONObject) 하는 도구
        JSONParser jsonParser = new JSONParser();
        // JSON 데이터를 나타내는 객체
        JSONObject jsonObject;

        try {
            // JSON 문자열을 분석하여 JSONObject 로 변환
            jsonObject = (JSONObject) jsonParser.parse(jsonString);
        } catch (
                ParseException e) {    // JSON 형식이 잘못되었거나 문자열이 깨졌다면, ParseException 발생
            throw new RuntimeException(e);
        }

        // 데이터를 저장할 맵 생성
        Map<String, Object> resultMap = new HashMap<>();

        // JSON 의 main 키에 해당하는 값을 가져오기 (JSON 구조는 계층적)
        JSONObject mainData = (JSONObject) jsonObject.get("main");
        // main 객체에서 temp 값을 추출하여 resultMap 에 추가
        resultMap.put("temp", mainData.get("temp"));

        JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
        JSONObject weatherData = (JSONObject) weatherArray.get(0);
        resultMap.put("main", weatherData.get("main"));
        resultMap.put("icon", weatherData.get("icon"));

        return resultMap;
    }
}
