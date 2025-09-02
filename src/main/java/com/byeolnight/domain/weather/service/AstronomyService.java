package com.byeolnight.domain.weather.service;

import com.byeolnight.domain.weather.dto.AstronomyEventResponse;
import com.byeolnight.domain.weather.entity.AstronomyEvent;
import com.byeolnight.domain.weather.repository.AstronomyEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
@Slf4j
public class AstronomyService {

    private final AstronomyEventRepository astronomyRepository;
    private final RestTemplate restTemplate;

    @Value("${nasa.api.key}")
    private String nasaApiKey;



    private static final String NASA_NEOWS_URL = "https://api.nasa.gov/neo/rest/v1/feed";
    private static final String NASA_DONKI_URL = "https://api.nasa.gov/DONKI";
    private static final String NASA_ISS_URL = "http://api.open-notify.org/iss-now.json";
    
    public Map<String, Object> getIssLocation() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(NASA_ISS_URL, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("ISS API 호출 실패: {}", e.getMessage(), e);
        }
        return Map.of("error", "ISS 데이터 조회 실패");
    }

    public List<AstronomyEventResponse> getUpcomingEvents() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<AstronomyEvent> events = astronomyRepository.findUpcomingEvents(thirtyDaysAgo);
        
        // 실제 NASA 데이터 우선, 예측 데이터는 후순위
        return events.stream()
                .sorted((e1, e2) -> {
                    boolean e1IsReal = isRealNasaData(e1);
                    boolean e2IsReal = isRealNasaData(e2);
                    
                    // 실제 데이터 우선
                    if (e1IsReal && !e2IsReal) return -1;
                    if (!e1IsReal && e2IsReal) return 1;
                    
                    // 같은 타입끼리는 최신순
                    return e2.getEventDate().compareTo(e1.getEventDate());
                })
                .limit(10)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    private boolean isRealNasaData(AstronomyEvent event) {
        LocalDateTime now = LocalDateTime.now();
        boolean isPastEvent = event.getEventDate().isBefore(now);
        boolean isRealType = event.getEventType().equals("ASTEROID") || 
                            event.getEventType().equals("SOLAR_FLARE") || 
                            event.getEventType().equals("GEOMAGNETIC_STORM");

        boolean isPrediction = event.getTitle().contains("예측") || 
                              event.getDescription().contains("예상") ||
                              event.getEventType().equals("MARS_WEATHER");
        
        return isPastEvent && isRealType && !isPrediction;
    }


    @Scheduled(cron = "0 0 9 * * ?") // 매일 오전 9시 실행
    public void fetchDailyAstronomyEvents() {
        performAstronomyDataCollection();
    }
    



    public void performAstronomyDataCollection() {
        try {
            log.info("천체 이벤트 데이터 수집 시작 (NASA API 연동)");

    
            deactivateOldEvents();

    
            fetchNasaAstronomyData();

            log.info("천체 이벤트 데이터 수집 완료");

        } catch (Exception e) {
            log.error("천체 이벤트 데이터 수집 실패: {}", e.getMessage());
            throw e;
        }
    }

    private void deactivateOldEvents() {

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<AstronomyEvent> oldEvents = astronomyRepository.findRecentEvents(thirtyDaysAgo);
        oldEvents = oldEvents.stream()
                .filter(event -> event.getEventDate().isBefore(thirtyDaysAgo))
                .collect(Collectors.toList());

        if (!oldEvents.isEmpty()) {
            astronomyRepository.deleteAll(oldEvents);
        }

        log.info("30일 이전 이벤트 {} 개 삭제", oldEvents.size());
    }

    private void fetchNasaAstronomyData() {
        List<AstronomyEvent> allEvents = new ArrayList<>();
        int successCount = 0;


        try {
            List<AstronomyEvent> neoEvents = fetchNeoWsData();
            allEvents.addAll(neoEvents);
            if (!neoEvents.isEmpty()) successCount++;
        } catch (Exception e) {
            log.warn("NASA NeoWs API 호출 실패: {}", e.getMessage(), e);
        }


        try {
            List<AstronomyEvent> donkiEvents = fetchDonkiData();
            allEvents.addAll(donkiEvents);
            if (!donkiEvents.isEmpty()) successCount++;
        } catch (Exception e) {
            log.warn("NASA DONKI API 호출 실패: {}", e.getMessage(), e);
        }




        if (!allEvents.isEmpty()) {
            log.info("저장 전 이벤트 목록:");
            for (AstronomyEvent event : allEvents) {
                String safeTitle = sanitizeForLog(event.getTitle());
                String safeType = sanitizeForLog(event.getEventType());
                log.info("- {} ({}): {}", safeTitle, safeType, event.getEventDate());
            }

            astronomyRepository.saveAll(allEvents);
            log.info("NASA API 데이터 {} 개 수집 성공 ({}/2 API 성공)", allEvents.size(), successCount);
        } else {
            log.warn("모든 NASA API 호출 실패");
        }
    }

    private List<AstronomyEvent> fetchNeoWsData() {
        List<AstronomyEvent> events = new ArrayList<>();

        if (nasaApiKey == null || nasaApiKey.trim().isEmpty() || "DEMO_KEY".equals(nasaApiKey)) {
            log.warn("NASA API 키가 설정되지 않음 또는 DEMO_KEY 사용 중");
            return events;
        }

        try {
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusDays(7);
            String neowsUrl = NASA_NEOWS_URL + "?start_date=" + startDate.toLocalDate() +
                    "&end_date=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;

            log.info("NASA NeoWs API 호출: {}", neowsUrl.replace(nasaApiKey, "***"));
            ResponseEntity<Map> response = restTemplate.getForEntity(neowsUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                events.addAll(parseNeoWsData(response.getBody()));
                log.info("NASA NeoWs 데이터 {} 개 수집 완료", events.size());
            } else {
                log.warn("NASA NeoWs API 응답 실패: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("NASA NeoWs API 호출 실패: {}", e.getMessage(), e);
            throw e;
        }

        return events;
    }

    private List<AstronomyEvent> fetchDonkiData() {
        List<AstronomyEvent> events = new ArrayList<>();

        if (nasaApiKey == null || nasaApiKey.trim().isEmpty() || "DEMO_KEY".equals(nasaApiKey)) {
            log.warn("NASA API 키가 설정되지 않음 또는 DEMO_KEY 사용 중");
            return events;
        }

        try {

            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = LocalDateTime.now();


            String flareUrl = NASA_DONKI_URL + "/FLR?startDate=" + startDate.toLocalDate() +
                    "&endDate=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;

            log.info("NASA DONKI FLR API 호출 (과거 30일)");
            ResponseEntity<Map[]> flareResponse = restTemplate.getForEntity(flareUrl, Map[].class);

            if (flareResponse.getStatusCode().is2xxSuccessful() && flareResponse.getBody() != null) {
                List<AstronomyEvent> flareEvents = parseDonkiFlareData(flareResponse.getBody());
                events.addAll(flareEvents);
                log.info("태양 플레어 데이터 {} 개 수집", flareEvents.size());
            }


            String gstUrl = NASA_DONKI_URL + "/GST?startDate=" + startDate.toLocalDate() +
                    "&endDate=" + endDate.toLocalDate() + "&api_key=" + nasaApiKey;

            log.info("NASA DONKI GST API 호출 (과거 30일)");
            ResponseEntity<Map[]> gstResponse = restTemplate.getForEntity(gstUrl, Map[].class);

            if (gstResponse.getStatusCode().is2xxSuccessful() && gstResponse.getBody() != null) {
                List<AstronomyEvent> gstEvents = parseDonkiGstData(gstResponse.getBody());
                events.addAll(gstEvents);
                log.info("지자기 폭풍 데이터 {} 개 수집", gstEvents.size());
            }

            log.info("NASA DONKI 데이터 {} 개 수집 완료", events.size());

        } catch (Exception e) {
            log.error("NASA DONKI API 호출 실패: {}", e.getMessage(), e);
            throw e;
        }

        return events;
    }

    private List<AstronomyEvent> fetchIssData() {
        List<AstronomyEvent> events = new ArrayList<>();

        try {
            log.info("NASA ISS API 호출: {}", NASA_ISS_URL);
            ResponseEntity<Map> response = restTemplate.getForEntity(NASA_ISS_URL, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AstronomyEvent issEvent = parseIssData(response.getBody());
                if (issEvent != null) {
                    events.add(issEvent);
                }
            } else {
                log.warn("NASA ISS API 응답 실패: {}", response.getStatusCode());
            }

            log.info("NASA ISS 데이터 {} 개 수집 완료", events.size());

        } catch (Exception e) {
            log.error("NASA ISS API 호출 실패: {}", e.getMessage());
            throw e;
        }

        return events;
    }



    private List<AstronomyEvent> fetchMarsWeatherData() {
        List<AstronomyEvent> events = new ArrayList<>();

        if (nasaApiKey == null || nasaApiKey.trim().isEmpty() || "DEMO_KEY".equals(nasaApiKey)) {
            log.warn("NASA API 키가 설정되지 않음");
            return events;
        }

        try {
            String marsUrl = "https://api.nasa.gov/insight_weather/?api_key=" + nasaApiKey + "&feedtype=json&ver=1.0";
            log.info("NASA Mars Weather API 호출");
            ResponseEntity<Map> response = restTemplate.getForEntity(marsUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> marsData = response.getBody();
                if (marsData.containsKey("sol_keys") && marsData.get("sol_keys") != null) {
                    events.add(createEvent("MARS_WEATHER", "화성 날씨 리포트",
                            "NASA InSight 탐사선이 전송한 화성의 최신 날씨 정보입니다. 화성의 온도, 바람, 기압 데이터를 확인할 수 있습니다.", 0, 15));
                    log.info("NASA Mars Weather 데이터 수집 성공");
                } else {
                    log.warn("NASA Mars Weather API: 데이터 없음 (InSight 미션 종료)");
                }
            }

        } catch (Exception e) {
            log.error("NASA Mars Weather API 호출 실패: {}", e.getMessage(), e);
        }

        return events;
    }

    private List<AstronomyEvent> parseDonkiFlareData(Map<String, Object>[] flareData) {
        if (flareData == null || flareData.length == 0) {
            log.info("태양 플레어 데이터 없음");
            return new ArrayList<>();
        }
        
        return Arrays.stream(flareData)
                .filter(flare -> flare.get("classType") != null && flare.get("beginTime") != null)
                .map(flare -> {
                    try {
                        String beginTime = (String) flare.get("beginTime");
                        LocalDateTime eventTime = LocalDateTime.parse(beginTime.replace("Z", ""));
                        return createAstronomyEvent("SOLAR_FLARE",
                                "태양 플레어 " + flare.get("classType") + " 등급",
                                "NASA DONKI에서 감지된 태양 플레어 활동입니다. 이 시기에 통신 및 GPS에 영향을 주었을 가능성이 있습니다.",
                                eventTime, "HIGH");
                    } catch (Exception e) {
                        log.warn("태양 플레어 데이터 파싱 실패: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<AstronomyEvent> parseDonkiGstData(Map<String, Object>[] gstData) {
        if (gstData == null || gstData.length == 0) {
            log.info("지자기 폭풍 데이터 없음");
            return new ArrayList<>();
        }
        
        return Arrays.stream(gstData)
                .filter(gst -> gst.get("startTime") != null)
                .map(gst -> {
                    try {
                        String startTime = (String) gst.get("startTime");
                        LocalDateTime eventTime = LocalDateTime.parse(startTime.replace("Z", ""));
                        return createAstronomyEvent("GEOMAGNETIC_STORM",
                                "지자기 폭풍 발생",
                                "NASA DONKI에서 감지된 지자기 폭풍입니다. 이 시기에 오로라 관측 기회가 증가했을 가능성이 있습니다.",
                                eventTime, "MEDIUM");
                    } catch (Exception e) {
                        log.warn("지자기 폭풍 데이터 파싱 실패: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private AstronomyEvent parseIssData(Map<String, Object> issData) {
        try {
            Map<String, Object> position = (Map<String, Object>) issData.get("iss_position");
            String latitude = (String) position.get("latitude");
            String longitude = (String) position.get("longitude");

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextPass = now.plusHours(2);

            return AstronomyEvent.builder()
                    .eventType("ISS_LOCATION")
                    .title("ISS 관측 기회")
                    .description(String.format("국제우주정거장이 한국 상공을 통과합니다. 맑은 점으로 5분간 관측 가능합니다. (현재 위치: %.1f°, %.1f°)",
                            Double.parseDouble(latitude), Double.parseDouble(longitude)))
                    .eventDate(nextPass)
                    .peakTime(nextPass.plusMinutes(2))
                    .visibility("WORLDWIDE")
                    .magnitude("MEDIUM")
                    .isActive(true)
                    .build();
        } catch (Exception e) {
            log.error("ISS 데이터 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private List<AstronomyEvent> parseNeoWsData(Map<String, Object> neowsData) {
        List<AstronomyEvent> events = new ArrayList<>();

        try {
            Map<String, Object> nearEarthObjects = (Map<String, Object>) neowsData.get("near_earth_objects");
            LocalDateTime now = LocalDateTime.now();

            for (Map.Entry<String, Object> entry : nearEarthObjects.entrySet()) {
                List<Map<String, Object>> asteroids = (List<Map<String, Object>>) entry.getValue();

                for (Map<String, Object> asteroid : asteroids.stream().limit(3).collect(Collectors.toList())) {
                    String name = (String) asteroid.get("name");
                    Boolean isPotentiallyHazardous = (Boolean) asteroid.get("is_potentially_hazardous_asteroid");



                    List<Map<String, Object>> closeApproachData = (List<Map<String, Object>>) asteroid.get("close_approach_data");
                    String distanceText = "정보 없음";
                    if (!closeApproachData.isEmpty()) {
                        Map<String, Object> missDistance = (Map<String, Object>) closeApproachData.get(0).get("miss_distance");
                        String kmDistance = (String) missDistance.get("kilometers");
                        distanceText = formatDistance(Double.parseDouble(kmDistance));
                    }

                    LocalDateTime eventDate = LocalDateTime.parse(entry.getKey() + "T21:00:00");


                    boolean isPastEvent = eventDate.isBefore(LocalDateTime.now());
                    String description = isPastEvent ? 
                        String.format("%s 소행성이 지구에서 %s 거리를 안전하게 통과했습니다. NASA에서 감지된 실제 이벤트입니다.",
                                isPotentiallyHazardous ? "잠재적 위험" : "안전한", distanceText) :
                        String.format("%s 소행성이 지구에서 %s 거리를 안전하게 통과할 예정입니다. 망원경으로 관측 가능합니다.",
                                isPotentiallyHazardous ? "잠재적 위험" : "안전한", distanceText);

                    events.add(AstronomyEvent.builder()
                            .eventType("ASTEROID")
                            .title("지구 근접 소행성 " + name.replace("(", "").replace(")", ""))
                            .description(description)
                            .eventDate(eventDate)
                            .peakTime(eventDate)
                            .visibility("WORLDWIDE")
                            .magnitude(isPotentiallyHazardous ? "HIGH" : "MEDIUM")
                            .isActive(true)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("NeoWs 데이터 파싱 실패: {}", e.getMessage());
        }

        return events;
    }

    private String formatDistance(double kilometers) {
        if (kilometers >= 1000000) {
            return String.format("%.1f백만 km", kilometers / 1000000);
        } else if (kilometers >= 10000) {
            return String.format("%.0f만 km", kilometers / 10000);
        } else {
            return String.format("%.0f km", kilometers);
        }
    }




    private AstronomyEvent createEvent(String type, String title, String description, int daysFromNow, int hour) {
        LocalDateTime eventDate = LocalDateTime.now().plusDays(daysFromNow);
        return AstronomyEvent.builder()
                .eventType(type)
                .title(title)
                .description(description)
                .eventDate(eventDate)
                .peakTime(eventDate.withHour(hour))
                .visibility("WORLDWIDE")
                .magnitude(type.contains("METEOR") || type.contains("SOLAR") ? "HIGH" : "MEDIUM")
                .isActive(true)
                .build();
    }

    private AstronomyEvent createAstronomyEvent(String type, String title, String description, LocalDateTime eventDate, String magnitude) {
        try {
            return AstronomyEvent.builder()
                    .eventType(type)
                    .title(title)
                    .description(description)
                    .eventDate(eventDate)
                    .peakTime(eventDate)
                    .visibility("WORLDWIDE")
                    .magnitude(magnitude)
                    .isActive(true)
                    .build();
        } catch (Exception e) {
            log.error("이벤트 생성 실패: {}", e.getMessage());
            return null;
        }
    }





    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\r\n\t]", "_").substring(0, Math.min(input.length(), 100));
    }
    
    private AstronomyEventResponse convertToResponse(AstronomyEvent event) {
        return AstronomyEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .peakTime(event.getPeakTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .visibility(event.getVisibility())
                .magnitude(event.getMagnitude())
                .isActive(event.getIsActive())
                .build();
    }
}