package com.svc.pokeguessteam.model.user;

import com.svc.pokeguessteam.model.enums.PokeballType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "TB_PROFILE_INVENTORY_ITEMS",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_PROFILE_INVENTORY_POKEBALL",
                columnNames = {"FK_PROFILE_ID", "POKEBALL_TYPE"}
        )
)
public class ProfileInventoryItemModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "PK_PROFILE_INVENTORY_ITEM_ID", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FK_PROFILE_ID", nullable = false)
    private ProfileModel profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "POKEBALL_TYPE", nullable = false, length = 30)
    private PokeballType pokeballType;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    public String getId() {
        return id;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void setProfile(ProfileModel profile) {
        this.profile = profile;
    }

    public PokeballType getPokeballType() {
        return pokeballType;
    }

    public void setPokeballType(PokeballType pokeballType) {
        this.pokeballType = pokeballType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
