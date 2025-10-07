package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    @InjectMocks
    private SecurityService securityService;

    private Sensor sensor;

    @BeforeEach
    void setup() {
        sensor = new Sensor("Front Door", SensorType.DOOR);
        securityService.addStatusListener(statusListener);
    }

    // --- Sensor activation rules ---

    @Test
    void armedSystem_activatesSensor_setsPendingAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void armedSystem_pendingAlarm_sensorActivated_setsAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void pendingAlarm_allSensorsInactive_setsNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        sensor.setActive(true);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);

        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }




    @Test
    void alarmActive_sensorChanges_noEffect() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    void pendingAlarm_sameSensorReactivated_setsAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void inactiveSensor_deactivatedAgain_noEffect() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }


    @Test
    void disarmedSystem_sensorActivated_noAlarmTriggered() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    void pendingAlarm_otherSensorsStillActive_noAlarmCleared() {
        Sensor other = new Sensor("Back Door", SensorType.DOOR);
        other.setActive(true);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor, other));

        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void alarmActive_sensorDeactivated_noAlarmCleared() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    @Test
    void armedHome_catDetected_setsAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void noCatDetected_andNoActiveSensors_setsNoAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));
        sensor.setActive(false);

        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    @Test
    void disarmed_thenCatDetected_thenArmedAway_resultsInAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);

        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void disarmed_thenCatDetected_thenArmedHome_resultsInAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void noCatDetected_doesNotSetCatFlag() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }


    @Test
    void disarmedSystem_setsNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void armedSystem_resetsAllSensorsInactive(ArmingStatus status) {
        sensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.setArmingStatus(status);

        assertFalse(sensor.getActive());
        verify(securityRepository).updateSensor(sensor);
    }


    @Test
    void getAlarmStatus_returnsRepositoryValue() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @Test
    void addSensor_delegatesToRepository() {
        securityService.addSensor(sensor);
        verify(securityRepository).addSensor(sensor);
    }

    @Test
    void removeSensor_delegatesToRepository() {
        securityService.removeSensor(sensor);
        verify(securityRepository).removeSensor(sensor);
    }

    @Test
    void removeStatusListener_noLongerReceivesUpdates() {
        securityService.removeStatusListener(statusListener);
        securityService.setAlarmStatus(AlarmStatus.ALARM);

        verify(statusListener, never()).notify(any());
    }


}
