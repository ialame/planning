// ============= ULIDGENERATOR - GENERATES UUID FROM ULID =============

//  REPLACE your UlidGenerator.java with this version:

package com.pcagrade.order.util;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.UUID;

/**
 *  Generate ULID compatible BINARY(16)
 *
* Generates ULIDs and converts them to UUIDs for MariaDB storage
* ULIDs retain their chronological order even when converted to UUIDs
*/
public class UlidGeneratorOld implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        try {
            // Générer un ULID monotonique (ordre chronologique garanti)
            Ulid ulid = UlidCreator.getMonotonicUlid();

            // Convertir en UUID pour stockage BINARY(16)
            UUID uuid = ulid.toUuid();

            System.out.println(" Generated ULID: " + ulid + " → UUID: " + uuid);

            return uuid; //  Retourner UUID pour compatibilité BINARY(16)

        } catch (Exception e) {
            System.err.println(" Erreur génération ULID: " + e.getMessage());
            // Fallback : UUID classique
            UUID fallback = UUID.randomUUID();
            System.out.println(" Fallback UUID: " + fallback);
            return fallback;
        }
    }
}
