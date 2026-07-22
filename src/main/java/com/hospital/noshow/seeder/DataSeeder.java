package com.hospital.noshow.seeder;

import com.hospital.noshow.entity.Doctor;
import com.hospital.noshow.entity.Patient;
import com.hospital.noshow.entity.User;
import com.hospital.noshow.enums.Gender;
import com.hospital.noshow.enums.UserRole;
import com.hospital.noshow.repository.DoctorRepository;
import com.hospital.noshow.repository.PatientRepository;
import com.hospital.noshow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping.");
            return;
        }

        // ── Admin ────────────────────────────────────────────────────────
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@hospital.com");
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);

        // ── Receptionist ─────────────────────────────────────────────────
        User rec = new User();
        rec.setUsername("receptionist");
        rec.setPassword(passwordEncoder.encode("rec123"));
        rec.setEmail("rec@hospital.com");
        rec.setRole(UserRole.RECEPTIONIST);
        userRepository.save(rec);

        // ── Doctor user + linked Doctor entity ──────────────────────────
        User docUser = new User();
        docUser.setUsername("doctor");
        docUser.setPassword(passwordEncoder.encode("doc123"));
        docUser.setEmail("doctor@hospital.com");
        docUser.setRole(UserRole.DOCTOR);
        userRepository.save(docUser);

        Doctor doctor = new Doctor();
        doctor.setName("Dr. Meera Sharma");
        doctor.setSpecialization("General Medicine");
        doctor.setUser(docUser);
        doctorRepository.save(doctor);

        // ── Patients: varying risk profiles ─────────────────────────────

        // Profile 1: HIGH-risk shape — chronic conditions + no SMS confirmation.
        // Matches §10 fallback rule: daysWaiting > 15 && !smsReceived && (hypertension || diabetes)
        Patient p1 = new Patient();
        p1.setName("Ramesh Kumar");
        p1.setAge(45);
        p1.setGender(Gender.M);
        p1.setDiabetes(true);
        p1.setHypertension(true);
        p1.setSmsReceived(false);
        p1.setScholarship(false);
        p1.setAlcoholism(false);
        p1.setEmail("ramesh@example.com");
        p1.setPhone("9876500001");
        patientRepository.save(p1);

        // Profile 2: LOW-risk shape — no chronic conditions, SMS confirmed, on scholarship.
        Patient p2 = new Patient();
        p2.setName("Sunita Devi");
        p2.setAge(29);
        p2.setGender(Gender.F);
        p2.setDiabetes(false);
        p2.setHypertension(false);
        p2.setAlcoholism(false);
        p2.setSmsReceived(true);
        p2.setScholarship(true);
        p2.setEmail("sunita@example.com");
        p2.setPhone("9876500002");
        patientRepository.save(p2);

        // Profile 3: MEDIUM-risk shape — alcoholism present but SMS confirmed,
        // no hypertension/diabetes, so it won't trip the chronic-condition HIGH clause.
        Patient p3 = new Patient();
        p3.setName("Arvind Yadav");
        p3.setAge(52);
        p3.setGender(Gender.M);
        p3.setDiabetes(false);
        p3.setHypertension(false);
        p3.setAlcoholism(true);
        p3.setSmsReceived(true);
        p3.setScholarship(false);
        p3.setEmail("arvind@example.com");
        p3.setPhone("9876500003");
        patientRepository.save(p3);

        // Profile 4: OTHER gender — exercises Gender.OTHER and §13's
        // encode_gender() majority-class (Female) mapping. Mild risk profile.
        Patient p4 = new Patient();
        p4.setName("Kiran Mehta");
        p4.setAge(34);
        p4.setGender(Gender.OTHER);
        p4.setDiabetes(false);
        p4.setHypertension(true);
        p4.setAlcoholism(false);
        p4.setSmsReceived(false);
        p4.setScholarship(false);
        p4.setEmail("kiran@example.com");
        p4.setPhone("9876500004");
        patientRepository.save(p4);

        log.info("Database seeded successfully: 3 users, 1 doctor, 4 patients.");
    }
}