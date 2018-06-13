package gov.cdc.foundation.helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TemplateHelper {

	@Value("${template.hl7.dateFormat}")
	private String dateFormat;
	@Value("${template.hl7.dateTimeFormat}")
	private String dateTimeFormat;
	@Value("#{'${template.hl7.surname}'.split(',')}")
	private List<String> surnames;
	@Value("#{'${template.hl7.givenname}'.split(',')}")
	private List<String> givennames;
	@Value("#{'${template.hl7.suffix}'.split(',')}")
	private List<String> suffixes;
	@Value("#{'${template.hl7.degree}'.split(',')}")
	private List<String> degrees;
	@Value("#{'${template.hl7.nameType}'.split(',')}")
	private List<String> nameTypes;
	@Value("#{'${template.hl7.sex}'.split(',')}")
	private List<String> sexes;
	@Value("${template.hl7.maximumNbOfRaces}")
	private int maximumNbOfRaces;
	@Value("#{'${template.hl7.race.id}'.split(',')}")
	private List<String> raceIds;
	@Value("#{'${template.hl7.race.name}'.split(',')}")
	private List<String> raceNames;
	@Value("#{'${template.hl7.city}'.split(',')}")
	private List<String> cities;
	@Value("#{'${template.hl7.state}'.split(',')}")
	private List<String> states;
	@Value("#{'${template.hl7.countyCode}'.split(',')}")
	private List<String> countyCodes;
	@Value("${template.hl7.maximumNbOfObservations}")
	private int maximumNbOfObservations;
	@Value("#{'${template.hl7.observationId}'.split(',')}")
	private List<String> observationIds;
	@Value("#{'${template.hl7.observationText}'.split(',')}")
	private List<String> observationTexts;
	@Value("#{'${template.hl7.observationCoding}'.split(',')}")
	private List<String> observationCodings;
	@Value("#{'${template.hl7.resultStatus}'.split(',')}")
	private List<String> resultStatuses;
	@Value("#{'${template.hl7.reasonForStudy.code}'.split(',')}")
	private List<String> reasonForStudyCodes;
	@Value("#{'${template.hl7.reasonForStudy.name}'.split(';')}")
	private List<String> reasonForStudyNames;
	@Value("#{'${template.hl7.jurisdictionCode}'.split(',')}")
	private List<String> jurisdictionCodes;
	@Value("#{'${template.hl7.jurisdictionName}'.split(',')}")
	private List<String> jurisdictionNames;

	public Map<String, Object> getModel() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("surname", surnames.get(ThreadLocalRandom.current().nextInt(surnames.size())));
		data.put("givenname", givennames.get(ThreadLocalRandom.current().nextInt(givennames.size())));
		data.put("suffix", suffixes.get(ThreadLocalRandom.current().nextInt(suffixes.size())));
		data.put("degree", degrees.get(ThreadLocalRandom.current().nextInt(degrees.size())));
		data.put("nameType", nameTypes.get(ThreadLocalRandom.current().nextInt(nameTypes.size())));
		LocalDate dob = LocalDate.now().minus(Period.ofDays((new Random().nextInt(365 * 80))));
		data.put("dob", dob.format(DateTimeFormatter.ofPattern(dateFormat)));
		data.put("sex", sexes.get(ThreadLocalRandom.current().nextInt(sexes.size())));

		// Generate the list of races
		int nbOfRaces = ThreadLocalRandom.current().nextInt(maximumNbOfRaces) + 1;
		data.put("nbOfRaces", nbOfRaces);
		List<Object> races = new ArrayList<>();
		List<Integer> indexedUsed = new ArrayList<>();
		for (int i = 0; i < nbOfRaces; i++) {
			Map<String, Object> race = new HashMap<String, Object>();
			int index = ThreadLocalRandom.current().nextInt(raceIds.size());
			if (!indexedUsed.contains(index)) {
				race.put("id", raceIds.get(index));
				race.put("name", raceNames.get(index));
				races.add(race);
				indexedUsed.add(index);
			}
		}
		data.put("races", races);

		data.put("city", cities.get(ThreadLocalRandom.current().nextInt(cities.size())));
		data.put("state", states.get(ThreadLocalRandom.current().nextInt(states.size())));
		data.put("zipCode", String.format("%05d", ThreadLocalRandom.current().nextInt(100000)));
		data.put("countyCode", countyCodes.get(ThreadLocalRandom.current().nextInt(countyCodes.size())));

		// Generate the list of observations
		int nbOfObservations = ThreadLocalRandom.current().nextInt(maximumNbOfObservations) + 1;
		data.put("nbOfObservations", nbOfObservations);
		List<Object> observations = new ArrayList<>();
		indexedUsed = new ArrayList<>();
		for (int i = 0; i < nbOfObservations; i++) {
			Map<String, Object> observation = new HashMap<String, Object>();
			int index = ThreadLocalRandom.current().nextInt(observationIds.size());
			if (!indexedUsed.contains(index)) {
				observation.put("index", indexedUsed.size() + 1);
				observation.put("id", observationIds.get(index));
				observation.put("text", observationTexts.get(index));
				observation.put("coding", observationCodings.get(ThreadLocalRandom.current().nextInt(observationCodings.size())));
				LocalDateTime observationDate = LocalDateTime.now().minus(Period.ofDays((new Random().nextInt(365))));
				observation.put("date", observationDate.format(DateTimeFormatter.ofPattern(dateTimeFormat)));
				LocalDateTime observationStatusChange = LocalDateTime.now().minus(Period.ofDays((new Random().nextInt(365))));
				observation.put("statusChange", observationStatusChange.format(DateTimeFormatter.ofPattern(dateTimeFormat)));
				observation.put("resultStatus", resultStatuses.get(ThreadLocalRandom.current().nextInt(resultStatuses.size())));
				int indexReason = ThreadLocalRandom.current().nextInt(reasonForStudyCodes.size());
				observation.put("reasonForStudyCode", reasonForStudyCodes.get(indexReason));
				observation.put("reasonForStudyName", reasonForStudyNames.get(indexReason));
				observations.add(observation);
				indexedUsed.add(index);
			}
		}
		data.put("observations", observations);

		// Generate the data for the OBX segments
		LocalDate dateOfIllness = LocalDate.now().minus(Period.ofDays((new Random().nextInt(60)) + 30));
		data.put("dateOfIllness", dateOfIllness.format(DateTimeFormatter.ofPattern(dateFormat)));
		LocalDate illnessEndDate = LocalDate.now().minus(Period.ofDays((new Random().nextInt(30))));
		data.put("illnessEndDate", illnessEndDate.format(DateTimeFormatter.ofPattern(dateFormat)));
		data.put("illnessDuration", ChronoUnit.DAYS.between(dateOfIllness, illnessEndDate));
		data.put("pregnancyStatus", new Random().nextInt(1) == 0);
		LocalDate diagnosisDate = LocalDate.now().minus(Period.ofDays((new Random().nextInt(60))));
		data.put("diagnosisDate", diagnosisDate.format(DateTimeFormatter.ofPattern(dateFormat)));
		data.put("hospitalized", new Random().nextInt(1) == 0);
		LocalDate admissionDate = LocalDate.now().minus(Period.ofDays((new Random().nextInt(60)) + 30));
		data.put("admissionDate", admissionDate.format(DateTimeFormatter.ofPattern(dateFormat)));
		LocalDate dischargeDate = LocalDate.now().minus(Period.ofDays((new Random().nextInt(30))));
		data.put("dischargeDate", dischargeDate.format(DateTimeFormatter.ofPattern(dateFormat)));
		data.put("durationOfStay", ChronoUnit.DAYS.between(admissionDate, dischargeDate));
		data.put("subjectDied", new Random().nextInt(1) == 0);
		data.put("stateCase", Calendar.getInstance().get(Calendar.YEAR) + "IN1000000" + String.format("%02d", new Random().nextInt(100)));
		data.put("legacyCase", String.format("%010d", new Random().nextInt(100000)));
		data.put("age", ChronoUnit.YEARS.between(dob, LocalDate.now()));
		LocalDate investigationStartDate = LocalDate.now().minus(Period.ofDays((new Random().nextInt(15))));
		data.put("investigationStartDate", investigationStartDate.format(DateTimeFormatter.ofPattern(dateFormat)));
		int jurisdictionIdx = ThreadLocalRandom.current().nextInt(jurisdictionCodes.size());
		data.put("jurisdictionCode", jurisdictionCodes.get(jurisdictionIdx));
		data.put("jurisdictionName", jurisdictionNames.get(jurisdictionIdx));

		return data;
	}
}
