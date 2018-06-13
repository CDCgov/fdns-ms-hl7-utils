MSH|^~\&|SendAppName^2.16.840.1.114222.nnnn^ISO|Sending-Facility^2.16.840.1.114222.nnnn^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20140630120030.1234-0500||ORU^R01^ORU_R01|TM_CN_TC0001a_HEP_Core|T|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~Hepatitis_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO
PID|1||9603544^^^MDCH&2.16.840.1.114222.4.1.3660&ISO||${surname}^${givenname}^^${suffix}^^${degree}^${nameType}||${dob}|${sex}||<#list races as race>${race.id}^${race.name}^CDCREC<#if race_has_next>~</#if></#list>|^^${city}^${state}^${zipCode}^USA^^^${countyCode}
<#list observations as observation>
OBR|${observation.index}||SAI^SendAppName^2.16.840.1.114222.TBD^ISO|${observation.id}^${observation.text}^${observation.coding}|||${observation.date}|||||||||||||||${observation.statusChange}|||${observation.resultStatus}||||||${observation.reasonForStudyCode}^${observation.reasonForStudyName}^NND
</#list>
OBX|1|TS|11368-8^Date of Illness Onset^LN||${dateOfIllness}||||||F
OBX|2|TS|77976-9^Illness End Date^LN||${illnessEndDate}||||||F
OBX|3|SN|77977-7^Illness Duration^LN||^${illnessDuration?string}|d^day^UCUM|||||F
OBX|4|CWE|77996-7^Pregnancy Status^LN||${pregnancyStatus?then('Y','N')}^${pregnancyStatus?then('Yes','No')}^HL70136||||||F
OBX|5|TS|77975-1^Diagnosis Date^LN||${diagnosisDate}||||||F
OBX|6|CWE|77974-4^Hospitalized^LN||${hospitalized?then('Y','N')}^${hospitalized?then('Yes','No')}^HL70136||||||F
OBX|7|TS|8656-1^Admission Date^LN||${admissionDate}||||||F
OBX|8|TS|8649-6^Discharge Date^LN||${dischargeDate}||||||F
OBX|9|SN|78033-8^Duration of Stay in days^LN||^${durationOfStay}||||||F
OBX|10|CWE|77978-5^Subject Died^LN||${subjectDied?then('Y','N')}^${subjectDied?then('Yes','No')}^HL70136||||||F
OBX|11|ST|77993-4^State Case Id^LN||${stateCase}||||||F
OBX|12|ST|77997-5^Legacy Case ID^LN||${legacyCase}||||||F
OBX|13|SN|77998-3^Patient Age^LN||^${age}|a^year^UCUM|||||F
OBX|14|ST|77969-4^Jurisdiction Code^LN||${jurisdictionCode}||||||F
OBX|15|DT|77979-3^Investigation Start Date^LN||${investigationStartDate}||||||F
OBX|16|CWE|77968-6^National Reporting Jurisdiction^LN||${jurisdictionCode}^${jurisdictionName}^FIPS5_2||||||F