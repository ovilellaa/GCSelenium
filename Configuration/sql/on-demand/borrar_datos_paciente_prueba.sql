-- Borra todos los datos de un episodio (customerid + processid) del paciente
-- de pruebas. @customerid y @processid son marcadores de SqlScriptRunner, no
-- variables de T-SQL: se sustituyen por texto antes de ejecutar (por eso van
-- entre comillas simples abajo, al ser columnas de texto), así que este
-- script SIEMPRE debe invocarse con ambos parámetros (nunca con Map.of()).
--
-- Uso: ver ClassBaseTest.resetTestPatientEpisode() — resuelve el customerid a
-- partir del NH configurado (pacqah1NH) y pide el processid (episodio) por
-- consola antes de/después de la suite, permitiendo omitir el borrado.

delete from
healthcareprocs..emergencyprocess
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..hospprocess
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..preoperativeindication
where customerid = '@customerid' and processid = '@processid'


delete from
drug..HealingTherapy
where customerid = '@customerid' and processid = '@processid'



delete from
general..CoronaryData
where customerid = '@customerid' and processid = '@processid'

delete from
general..CaseConference
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..bloodpressure
where customerid = '@customerid' and processid = '@processid'

delete from
general..RespiratoryData
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..fevervalues
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..Glycaemia
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..Overcrowding
where customerid = '@customerid' and processid = '@processid'

delete from
general..GroupProcess
where customerid = '@customerid' and processid = '@processid'

delete from
general..dischargesummary
where customerid = '@customerid' and processid = '@processid'
delete from
general..DoctorCodedDiagnosis
where customerid = '@customerid' and processid = '@processid'
delete from
drug..drugtherapyfavorite
where customerid = '@customerid' and processid = '@processid'
delete from
drug..TherapyFavorite
where customerid = '@customerid' and processid = '@processid'
delete from
healthcareprocs..anamnesis
where customerid = '@customerid' and processid = '@processid'
delete from
healthcareprocs..customerevolution
where customerid = '@customerid' and processid = '@processid'
delete from
healthcareprocs..medicalrecord
where customerid = '@customerid' and processid = '@processid'
delete from
HistoricalFile..HistDrugTherapy
where customerid = '@customerid' and processid = '@processid'
delete from
HistoricalFile..HistGivenAdministration
where customerid = '@customerid' and processid = '@processid'
delete from
HistoricalFile..HistTherapy
where customerid = '@customerid' and processid = '@processid'
delete from
tests..labmicrogenrequest
where customerid = '@customerid' and processid = '@processid'
delete from
tests..radiology
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..Balances
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..ProgrammedTreatment
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..EmergencyClassification
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..SelectionChart
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistGivenAdministrationOxygen
where customerid =  '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistVentTherapy
where customerid =  '@customerid' and processid = '@processid'

delete from
drug..StandardPrescription
where customerid = '@customerid' and processid = '@processid'


delete from
General..EffectClinicalGrading
where customerid =  '@customerid' and process = '@processid'

delete from
general..DischargeAdendum
where customerid =  '@customerid' and processid = '@processid'


delete from
HistoricalFile..RadiologyErased
where customerid =  '@customerid' and processid = '@processid'

delete from
nursing..Actions
where customerid =  '@customerid' and processid = '@processid'

delete from
nursing..IVClass
where customerid =  '@customerid' and processid = '@processid'

delete from
nursing..EmptyingKind
where customerid =  '@customerid' and processid = '@processid'

delete from
nursing..Lesion
where customerid =  '@customerid' and processid = '@processid'

delete from
nursing..NursingDiag
where customerid =  '@customerid' and processid = '@processid'

delete from
nursing..previousstate
where customerid =  '@customerid' and processid = '@processid'

delete from
nursing..Progress
where customerid =  '@customerid' and processid = '@processid'

delete from
healthcareprocs..AppSummary
where customerid =  '@customerid' and processid = '@processid'

delete from
healthcareprocs..Diets
where customerid =  '@customerid' and processid = '@processid'

delete from
healthcareprocs..Feed
where customerid =  '@customerid' and processid = '@processid'

delete from
general..respiratorydata
where customerid =  '@customerid' and processid = '@processid'

delete from
tests..commontest
where customerid =  '@customerid' and processid = '@processid'

delete from
General..DetailedClinicalGrading
where customerid =  '@customerid'

delete from
HistoricalFile..HistDrugsAdministration
where customerid =  '@customerid'



delete from
configuration..emergencyboxes 
where  customerid =  '@customerid' and processid = '@processid'

delete from
configuration..EmergencyObservation 
where  customerid =  '@customerid' and processid = '@processid'

delete from
general..DoctorCodedDiagnosis 
where  customerid =  '@customerid' and processid = '@processid'


-- ============================================================
-- A partir de aqui: tablas anadidas automaticamente a partir de
-- DatosEpisodioClinico.sql (selects que no estaban cubiertos por
-- el borrado anterior). Mismo filtro customerid/processid salvo
-- las excepciones marcadas explicitamente.
-- ============================================================

-- patientmanagement
delete from
patientmanagement..AnnotationaAutoApp
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..annotationaccidents
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..Appointments
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..AppointmentsClarifications
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..AppointmentsHCenter
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..AppointmentUsersHistory
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..AssistanceAgree
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..Canceled
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..CanceledOncoTreatSessions
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..DeferredAppointments
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..ExtraBed
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..ForecastBeds
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..HistoricBeds
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..NoFinancedAppointments
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..oncotreatsessions
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..RoomChangesCustomer
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..SurgicallyWaitingList
where customerid = '@customerid' and processid = '@processid'

delete from
patientmanagement..SurgicallyWaitingListExit
where customerid = '@customerid' and processid = '@processid'


-- Resources
delete from
Resources..AdendumClinicalPathology
where customerid = '@customerid' and processid = '@processid'

delete from
Resources..AdendumRadiology
where customerid = '@customerid' and processid = '@processid'

delete from
Resources..AdendumSummaryDoctorRel
where customerid = '@customerid' and processid = '@processid'

delete from
Resources..GraphicAssistantPRocess
where customerid = '@customerid' and processid = '@processid'

delete from
Resources..SummaryDoctorRel
where customerid = '@customerid' and processid = '@processid'

delete from
Resources..TransfussionDoctorRel
where customerid = '@customerid' and processid = '@processid'


-- surgery
delete from
surgery..AfterDischargeInterview
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..Analgesia
where customerid = '@customerid' and processid = '@processid'

delete e
from
	surgery..anestesicagent e
	inner join surgery..anesth i on e.[KEY]=i.interiors
where
	i.customerid = '@customerid' and i.processid = '@processid'
-- NOTA: depende de surgery..anesth (join); debe ejecutarse ANTES de borrar surgery..anesth.

delete a
from
	surgery..anestesic a
	inner join surgery..anesth i on a.anesthesiareferenceid=i.interiors
where
	i.customerid = '@customerid' and i.processid = '@processid'
-- NOTA: depende de surgery..anesth (join); debe ejecutarse ANTES de borrar surgery..anesth.

delete from
surgery..anesth
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..AnesthReportDet
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..APGARScore
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..BeforeSurgInterview
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..DeliveryDetail
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..EcographSurg
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..ImmediatePuerperium
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..ObstetricaCateterAnalgesia
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..ObstetricAnalgesia
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..OperatingTheatreBook
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..OperatingTheatrePlanning
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..OperationTheatherRecord
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..OutCustomerSurgEvolutionRecord
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..PACUAnesth
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..PACUAnesthReportDet
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..Partogram
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..PassiveCitationsFollowUp
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..PlacentaDelivery
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..PreoperatoryDiagnosis
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..PreoperatoryOMC
where PatientId = '@customerid' and processid = '@processid'

delete from
surgery..ResqueBolusDose
where CUSTOMERID = '@customerid' and processid = '@processid'

delete from
surgery..SProcedure
where CUSTOMERID = '@customerid' and processid = '@processid'

delete from
surgery..surgerydiagnosis
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..SurgeryRecord
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..SurgeryReportAdendum
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..surgical
where customerid = '@customerid' and processid = '@processid'

delete from
surgery..SurgicalOMC
where PatientId = '@customerid' and processid = '@processid'

delete from
surgery..VentilatorySupport
where customerid = '@customerid' and processid = '@processid'


-- general
delete from
general..Adendums
where customerid = '@customerid' and processid = '@processid'

delete from
general..AdmissionCodedDiagnosis
where customerid = '@customerid' and processid = '@processid'

delete from
general..AdmissionSummary
where customerid = '@customerid' and processid = '@processid'

delete from
general..AppointmentsDiagnosis
where customerid = '@customerid' and processid = '@processid'

delete from
general..BenefitsServices
where customerid = '@customerid' and processid = '@processid'

delete from
general..CaCodedDiagnosis
where customerid = '@customerid' and processid = '@processid'

delete from
general..CAMethod
where customerid = '@customerid' and processid = '@processid'

delete from
general..CrashTypeRecords
where customerid = '@customerid' and processid = '@processid'

delete from
general..CustomerAdvices
where customerid = '@customerid' and processid = '@processid'

delete from
general..CustomerClosesGuides
where customerid = '@customerid' and processid = '@processid'

delete from
general..DocumentalistCodedDiagnosis
where customerid = '@customerid' and processid = '@processid'

delete from
general..DrugTherapyHistory
where customerid = '@customerid' and processid = '@processid'

delete from
general..ExportHospitalization
where customerid = '@customerid' and processid = '@processid'

delete from
general..FemalePhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
general..HistoryCustomerAdvides
where customerid = '@customerid' and processid = '@processid'

delete from
general..MaternalChildFeeding
where customerid = '@customerid' and processid = '@processid'

delete from
general..MedicalLink
where customerid = '@customerid' and processid = '@processid'

delete from
general..MetaChanges
where customerid = '@customerid' and processid = '@processid'

delete from
general..MetaDataForms
where customerid = '@customerid' and processid = '@processid'

delete from
general..Newborn
where customerid = '@customerid' and processid = '@processid'

delete from
general..OncoTreatAdmin
where customerid = '@customerid' and processid = '@processid'

delete from
general..RadiologyExpense
where customerid = '@customerid' and processid = '@processid'

delete from
general..ReferenceInformation
where customerid = '@customerid' and processid = '@processid'

delete from
general..RehabTherapy
where customerid = '@customerid' and processid = '@processid'

delete from
general..RehabTherapyGravity
where customerid = '@customerid' and processid = '@processid'

delete from
general..TraceCustomerMessage
where customerid = '@customerid' and processid = '@processid'

delete from
general..Tumors
where customerid = '@customerid' and processid = '@processid'

delete from
general..UsersMessages
where customerid = '@customerid' and processid = '@processid'

delete from
general..WrittenDamage
where customerid = '@customerid' and processid = '@processid'


-- bloodbanking
delete from
bloodbanking..BBBGReg
where customerid = '@customerid' and processid = '@processid'

delete from
bloodbanking..BBCrossMatching
where customerid = '@customerid' and processid = '@processid'

delete from
bloodbanking..transaux
where customerid = '@customerid' and processid = '@processid'

delete from
bloodbanking..TransfRequestMasters
where customerid = '@customerid' and processid = '@processid'

delete from
bloodbanking..UnitsRecord
where customerid = '@customerid' and processid = '@processid'


-- nursing
delete from
nursing..ActionModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..ActionRestriction
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..AilmentBehavior
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..AnalysisAttention
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..AuxDataVal
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..BasalOutp
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..CancellationModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..Catheter
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..catheterkind
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..ClinicalBackground
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..CognizantModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..CognizantPerception
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..ConductModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..DermisStatus
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..DischargeReport
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..Examination
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..Excrement
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..ExcretionDifficulty
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..HealthinessModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..HealthinessStandard
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..IdeologyModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..Inconvenient
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..mainbloodpressure
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..medicalrecords
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..MovesMenagement
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..NosocomialEntry
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..NutritionalModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..PainScale
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..PediatricModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..PediatricModelSpec
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..ProgrammedTreatmentSpec
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..ProgressSpecification
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..Report
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..RestModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..SelfperceptionModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..SexualityModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..StrainCapacity
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..StrainModel
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..TreatmentSpecification
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..UrinationDifficulty
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..urinevolume
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..ValoresDiarios
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..VenerealAilments
where customerid = '@customerid' and processid = '@processid'

delete from
nursing..venousaccessdischargereport
where customerid = '@customerid' and processid = '@processid'


-- drug
delete from
drug..Carts
where customerid = '@customerid' and processid = '@processid'

delete from
drug..ClosedCarts
where customerid = '@customerid' and processid = '@processid'

delete from
drug..DischargeTherapy
where customerid = '@customerid' and processid = '@processid'

delete t
from
	drug..DrugDispensation t
	inner join Drug..ChemDispensation c on c.DISPENSATIONKEY=t.CHEMDISPENSATIONKEY
where
	c.customerid = '@customerid' and t.processid = '@processid'

delete from
drug..DrugsAdministration
where customerid = '@customerid' and processid = '@processid'

delete from
drug..drugtherapy
where customerid = '@customerid' and processid = '@processid'

delete from
drug..ElectDrugTherapy
where customerid = '@customerid' and processid = '@processid'

delete from
drug..GivenAdministration
where customerid = '@customerid' and processid = '@processid'

delete from
drug..GivenAdministrationOxygen
where customerid = '@customerid' and processid = '@processid'

delete from
drug..HistVentTherapy
where customerid = '@customerid' and processid = '@processid'

delete from
drug..OncoTherapyDetail
where customerid = '@customerid' and processid = '@processid'

delete from
drug..OncoTherapyMaster
where customerid = '@customerid' and processid = '@processid'

delete from
drug..OncoTreatPreparation
where customerid = '@customerid' and processid = '@processid'

delete from
drug..OxygenAdministration
where customerid = '@customerid' and processid = '@processid'

delete from
drug..packages
where customerid = '@customerid' and processid = '@processid'

delete from
drug..PrescriptionHistory
where customerid = '@customerid' and processid = '@processid'

delete from
drug..ProcessingCarts
where customerid = '@customerid' and processid = '@processid'

delete from
drug..ScheduleOncoTherapy
where customerid = '@customerid' and processid = '@processid'

delete from
drug..Therapy
where customerid = '@customerid' and processid = '@processid'

delete from
drug..UniCount
where customerid = '@customerid' and processid = '@processid'

delete from
drug..UnidosisReport
where customerid = '@customerid' and processid = '@processid'

delete from
drug..UnidosisSummary
where customerid = '@customerid' and processid = '@processid'

delete from
drug..UnidosisVariations
where customerid = '@customerid' and processid = '@processid'

delete from
drug..VentTherapy
where customerid = '@customerid' and processid = '@processid'


-- healthcareprocs
delete from
healthcareprocs..AdvancedOphPhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..BabyFood
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..BasalOutp
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..ENTPhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..EyePressure
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..Fasting
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..genericphysicalexamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..IncomePhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..MaxSurgeryPhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..NeoPhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..PCPhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..PedPhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..ProgressPhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..SimpleOphPhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..SurgeriesPhysicalExamination
where customerid = '@customerid' and processid = '@processid'

delete from
healthcareprocs..surgicalSummary
where customerid = '@customerid' and processid = '@processid'


-- HistoricalFile
delete from
HistoricalFile..Anesth_Del
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..CommonTestErased
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..Consultas_TratamientoHDia
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..Consultas_TratamientoHDiaCalendario
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..Consultas_TratamientoHDiaDetalle
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..ConsumptionAccumulate
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..DeletedOutCustomerProcess
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..DeletedOutCustomerProcess
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..DRGAccumulate
where HISTORYNUMBER = '@historynumber' and processid = '@processid'
-- NOTA: esta tabla filtra por HISTORYNUMBER, no por customerid; revisar si el runner debe pasar @historynumber.

delete from
HistoricalFile..EndoscopyErased
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistAppointments
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistBalances
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistBloodPressure
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistCommonTest
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistCoronaryData
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistFeverValues
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistGlycaemia
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistLabMicroGenRequest
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistMainBloodPressure
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistOncoTherapyDetail
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistOncoTherapyMaster
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistOncoTreatAdmin
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistOvercrowding
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistOxygenAdministration
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistPainScale
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistRespiratoryData
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistScheduleOncoTherapy
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..HistUrineVolume
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..Newborn_Del
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..OperatingTheatrePlanningErase
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..PACUAnesth_Del
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..PlacentaDelivery_Del
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..ProgrammedTreatment_Del
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..ProgrammedTreatmentSpec_Del
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..TraceEmergencyClassification
where customerid = '@customerid' and processid = '@processid'

delete from
HistoricalFile..TraceOutCustomerProcess
where customerid = '@customerid' and processid = '@processid'


-- tests
delete from
tests..clinicalpathology
where customerid = '@customerid' and processid = '@processid'

delete from
tests..endoscopy
where customerid = '@customerid' and processid = '@processid'


-- configuration
delete from
configuration..Plantesting
where customerid = '@customerid' and processid = '@processid'

