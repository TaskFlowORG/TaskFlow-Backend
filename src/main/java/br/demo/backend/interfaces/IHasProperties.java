package br.demo.backend.interfaces;

import br.demo.backend.model.properties.Property;

import java.util.Collection;

public interface IHasProperties {

    Collection<Property> getProperties();
}
