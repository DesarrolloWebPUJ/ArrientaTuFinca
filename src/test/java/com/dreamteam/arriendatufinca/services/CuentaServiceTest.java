package com.dreamteam.arriendatufinca.services;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.dreamteam.arriendatufinca.dtos.CuentaDTO;
import com.dreamteam.arriendatufinca.entities.Cuenta;
import com.dreamteam.arriendatufinca.enums.Estado;
import com.dreamteam.arriendatufinca.exception.ManejadorErrores;
import com.dreamteam.arriendatufinca.repository.CuentaRepository;

public class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private UtilityService utilityService;

    @InjectMocks
    private CuentaService cuentaService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllCuentas() {
        Cuenta cuenta1 = new Cuenta("cuenta1", "contrasena1", "cuenta1@example.com");
        Cuenta cuenta2 = new Cuenta("cuenta2", "contrasena2", "cuenta2@example.com");

        when(cuentaRepository.findAll()).thenReturn(Arrays.asList(cuenta1, cuenta2));

        CuentaDTO cuentaDTO1 = new CuentaDTO();
        cuentaDTO1.setNombreCuenta("cuenta1");
        CuentaDTO cuentaDTO2 = new CuentaDTO();
        cuentaDTO2.setNombreCuenta("cuenta2");

        when(modelMapper.map(cuenta1, CuentaDTO.class)).thenReturn(cuentaDTO1);
        when(modelMapper.map(cuenta2, CuentaDTO.class)).thenReturn(cuentaDTO2);

        List<CuentaDTO> cuentas = cuentaService.get();

        assertThat(cuentas).hasSize(2);
        assertThat(cuentas.get(0).getNombreCuenta()).isEqualTo("cuenta1");
        assertThat(cuentas.get(1).getNombreCuenta()).isEqualTo("cuenta2");
    }

    @Test
    public void testGetCuentaById() {
        Cuenta cuenta = new Cuenta("cuenta1", "contrasena1", "cuenta1@example.com");
        cuenta.setIdCuenta(1);

        when(cuentaRepository.findById(1)).thenReturn(Optional.of(cuenta));

        CuentaDTO cuentaDTO = new CuentaDTO();
        cuentaDTO.setIdCuenta(1);
        cuentaDTO.setNombreCuenta("cuenta1");

        when(modelMapper.map(cuenta, CuentaDTO.class)).thenReturn(cuentaDTO);

        ResponseEntity<CuentaDTO> response = cuentaService.get(1);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIdCuenta()).isEqualTo(1);
        assertThat(response.getBody().getNombreCuenta()).isEqualTo("cuenta1");
    }

    @Test
    public void testGetCuentaById_NotFound() {
        when(cuentaRepository.findById(1)).thenReturn(Optional.empty());

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ManejadorErrores.ERROR_CUENTA_NO_EXISTE))
            .when(utilityService).verificarAusencia(any(), eq(ManejadorErrores.ERROR_CUENTA_NO_EXISTE));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            cuentaService.get(1);
        });

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo(ManejadorErrores.ERROR_CUENTA_NO_EXISTE);
    }

    @Test
    public void testGetCuentaByEmail() {
        Cuenta cuenta = new Cuenta("cuenta1", "contrasena1", "cuenta1@example.com");

        when(cuentaRepository.findByEmail("cuenta1@example.com")).thenReturn(Optional.of(cuenta));

        CuentaDTO cuentaDTO = new CuentaDTO();
        cuentaDTO.setEmail("cuenta1@example.com");

        when(modelMapper.map(cuenta, CuentaDTO.class)).thenReturn(cuentaDTO);

        ResponseEntity<CuentaDTO> response = cuentaService.get("cuenta1@example.com");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("cuenta1@example.com");
    }

    @Test
    public void testGetCuentaByEmail_NotFound() {
        when(cuentaRepository.findByEmail("cuenta1@example.com")).thenReturn(Optional.empty());

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ManejadorErrores.ERROR_CORREO_CUENTA_NO_EXISTE))
            .when(utilityService).verificarAusencia(any(), eq(ManejadorErrores.ERROR_CORREO_CUENTA_NO_EXISTE));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            cuentaService.get("cuenta1@example.com");
        });

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo(ManejadorErrores.ERROR_CORREO_CUENTA_NO_EXISTE);
    }

    @Test
    public void testUpdateCuenta() {
        Cuenta cuenta = new Cuenta("cuenta1", "contrasena1", "cuenta1@example.com");
        cuenta.setIdCuenta(1);

        when(cuentaRepository.findById(1)).thenReturn(Optional.of(cuenta));

        CuentaDTO cuentaDTO = new CuentaDTO();
        cuentaDTO.setIdCuenta(1);
        cuentaDTO.setNombreCuenta("cuenta1_updated");

        when(modelMapper.map(cuenta, CuentaDTO.class)).thenReturn(cuentaDTO);
        when(cuentaRepository.save(any(Cuenta.class))).thenReturn(cuenta);

        ResponseEntity<CuentaDTO> response = cuentaService.update(cuentaDTO);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getNombreCuenta()).isEqualTo("cuenta1_updated");
        verify(cuentaRepository).save(cuenta);
    }

    @Test
    public void testUpdateCuenta_NotFound() {
        CuentaDTO cuentaDTO = new CuentaDTO();
        cuentaDTO.setIdCuenta(1);

        when(cuentaRepository.findById(1)).thenReturn(Optional.empty());

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ManejadorErrores.ERROR_CUENTA_NO_EXISTE))
            .when(utilityService).verificarAusencia(any(), eq(ManejadorErrores.ERROR_CUENTA_NO_EXISTE));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            cuentaService.update(cuentaDTO);
        });

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo(ManejadorErrores.ERROR_CUENTA_NO_EXISTE);
    }

    @Test
    public void testUpdateContrasena() {
        Cuenta cuenta = new Cuenta("cuenta1", "contrasena1", "cuenta1@example.com");
        cuenta.setIdCuenta(1);

        when(cuentaRepository.findById(1)).thenReturn(Optional.of(cuenta));

        CuentaDTO cuentaDTO = new CuentaDTO();
        cuentaDTO.setIdCuenta(1);

        when(modelMapper.map(cuenta, CuentaDTO.class)).thenReturn(cuentaDTO);
        when(cuentaRepository.save(any(Cuenta.class))).thenReturn(cuenta);

        ResponseEntity<CuentaDTO> response = cuentaService.updateContrasena(cuentaDTO, "contrasena1", "nuevaContrasena");

        assertThat(response.getBody()).isNotNull();
        assertThat(cuenta.getContrasena()).isEqualTo("nuevaContrasena");
        verify(cuentaRepository).save(cuenta);
    }

    @Test
    public void testUpdateContrasena_IncorrectPassword() {
        Cuenta cuenta = new Cuenta("cuenta1", "contrasena1", "cuenta1@example.com");
        cuenta.setIdCuenta(1);

        when(cuentaRepository.findById(1)).thenReturn(Optional.of(cuenta));

        CuentaDTO cuentaDTO = new CuentaDTO();
        cuentaDTO.setIdCuenta(1);

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, ManejadorErrores.ERROR_CONTRASENA_INCORRECTA))
            .when(utilityService).devolverUnuthorized(ManejadorErrores.ERROR_CONTRASENA_INCORRECTA);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            cuentaService.updateContrasena(cuentaDTO, "wrongPassword", "nuevaContrasena");
        });

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo(ManejadorErrores.ERROR_CONTRASENA_INCORRECTA);
    }

    @Test
    public void testDeleteCuenta() {
        Cuenta cuenta = new Cuenta("cuenta1", "contrasena1", "cuenta1@example.com");
        cuenta.setIdCuenta(1);

        when(cuentaRepository.findById(1)).thenReturn(Optional.of(cuenta));

        cuentaService.deleteCuenta(1);

        verify(cuentaRepository).save(cuenta);
        assertThat(cuenta.getEstado()).isEqualTo(Estado.INACTIVE);
    }

    @Test
    public void testDeleteCuenta_NotFound() {
        when(cuentaRepository.findById(1)).thenReturn(Optional.empty());

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ManejadorErrores.ERROR_CUENTA_NO_EXISTE))
            .when(utilityService).verificarAusencia(any(), eq(ManejadorErrores.ERROR_CUENTA_NO_EXISTE));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            cuentaService.deleteCuenta(1);
        });

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo(ManejadorErrores.ERROR_CUENTA_NO_EXISTE);
    }
}
