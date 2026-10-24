package ru.java.programming.dz.task.dz2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;


public class YandexCloud {

    public static final int LIMIT = 10;

    public static final String MOSCOW_LON = "37.61993";

    public static final String MOSCOW_LAT = "55.75649";

    public static final String URL = "https://api.weather.yandex.ru/v2/forecast?lat=%s&lon=%s";

    public static final String X_YANDEX_WEATHER_KEY = "X-Yandex-Weather-Key";

    private static final String KEY = "461f89dd-ef74-409c-a8ae-a916ad485090";


    static void main() {
        String response = getResponse(MOSCOW_LAT, MOSCOW_LON);
        if (response == null) {
            System.out.println("Не удалось получить информацию о погоде");
            return;
        }
        System.out.println("Весь ответ сервиса «как есть»: " + response);
        ResponseDto responseDto = mapResponseDto(response);
        if (responseDto == null) {
            return;
        }
        writeTemp(responseDto);
        writeTempAvg(responseDto, LIMIT);
    }

    private static String getResponse(String lat, String lon) {
        String resultUrl = URL.formatted(lat, lon);
        HttpResponse<String> response = null;
        try (HttpClient httpClient = HttpClient.newBuilder().build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resultUrl))
                    .header(X_YANDEX_WEATHER_KEY, KEY)
                    .GET()
                    .build();
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (IOException | InterruptedException e) {
            System.out.println("Ошибка при отправке запроса - " + resultUrl);
        }
        if (response == null || response.body() == null || response.body().isEmpty()) {
            System.out.println("Получено пустое тело ответа");
            return null;
        }
        if (!is2xx(response.statusCode())) {
            System.out.println("Результат запроса не удачный. Стату код - %s. Тело ответа - %s".formatted(response.statusCode(), response.body()));
            return null;
        }
        return response.body();
    }

    private static ResponseDto mapResponseDto(String response) {
        ObjectMapper objectMapper = new ObjectMapper();
        ResponseDto responseDto;
        try {
            responseDto = objectMapper.readValue(response, ResponseDto.class);
        } catch (JsonProcessingException e) {
            System.out.println("Не удалось дессериализовать ответ. Информация о погоде получена в неизвестном формате");
            return null;
        }
        return responseDto;
    }

    private static boolean is2xx(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private static void writeTemp(ResponseDto responseDto) {
        if (responseDto == null || responseDto.getFact() == null) {
            System.out.println("Значение fact.temp не получено в ответе");
            return;
        }
        System.out.println("Значение fact.temp - " + responseDto.getFact().getTemp());
    }

    private static void writeTempAvg(ResponseDto responseDto, int limit) {
        if (responseDto == null || responseDto.getForecasts() == null || responseDto.getForecasts().isEmpty()) {
            System.out.println("Не получено ни одного заполненного forecasts");
            return;
        }
        List<ResponseDto.ForecastsDto> forecasts = responseDto.getForecasts();
        List<Integer> tempsAvg = forecasts.stream()
                .map(x -> x == null ? null : x.getParts())
                .filter(Objects::nonNull)
                .map(ResponseDto.PartsDto::getDay)
                .filter(Objects::nonNull)
                .map(ResponseDto.DayDto::getTempAvg)
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
        Integer tempAvgResult = null;
        for (Integer tempAvg : tempsAvg) {
            if (tempAvgResult == null) {
                tempAvgResult = tempAvg;
                continue;
            }
            tempAvgResult += tempAvg;
        }
        if (tempAvgResult == null) {
            System.out.println("Не получено ни одного заполненного forecasts[].parts.day.temp_avg");
            return;
        }
        int divider = Math.min(limit, tempsAvg.size());
        System.out.println("Среднее арифметическое полученных forecasts[].parts.day.temp_avg - " + tempAvgResult / divider);
    }


    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ResponseDto {

        private FactDto fact;

        private List<ForecastsDto> forecasts;

        @Setter
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class FactDto {

            private Integer temp;
        }

        @Setter
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class ForecastsDto {

            private PartsDto parts;
        }

        @Setter
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class PartsDto {

            private DayDto day;
        }

        @Setter
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class DayDto {

            @JsonProperty("temp_avg")
            private Integer tempAvg;
        }
    }
}
