package br.demo.backend.model.values;

import br.demo.backend.exception.DeserializerException;
import br.demo.backend.model.User;
import br.demo.backend.model.enums.TypeOfProperty;
import br.demo.backend.model.properties.Option;
import br.demo.backend.model.properties.Property;
import br.demo.backend.model.relations.PropertyValue;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class DeserializerValue extends StdDeserializer<PropertyValue> {
    JsonNode jsonNode;


    protected DeserializerValue() {
        super(PropertyValue.class);
    }

    @Override
    public PropertyValue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {

        Long id = null;
        jsonNode = deserializationContext.readTree(jsonParser);
        try {
            id = jsonNode.get("id").asLong();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        if (isPresent(jsonNode, "property")) {
            JsonNode jsonProp = jsonNode.get("property");
            if (isPresent(jsonProp, "type")) {
                String type = jsonProp.get("type").asText();
                Long idProp = null;
                try {
                    idProp = jsonProp.get("id").asLong();
                } catch (Exception e) {
                    System.out.println("Deu erro no id da prop");
                }
                if (isPresent(jsonNode, "value")) {
                    JsonNode jsonValue = jsonNode.get("value");
                    if (isPresent(jsonValue, "value")) {
                        JsonNode value = jsonValue.get("value");
                        Long idTaskVl = null;
                        try {
                            idTaskVl = jsonValue.get("id").asLong();
                        } catch (Exception e) {
                            System.out.println("Deu erro no id da propValue");
                        }
                        Property property = new Property(idProp, TypeOfProperty.valueOf(type));
                        if (type.equals("TEXT")) {
                            return new PropertyValue(id, property,  new TextValued(idTaskVl, value.asText()));
                        }
                        else if(type.equals("ARCHIVE")){
                            return new PropertyValue(id, property,  new ArchiveValued(idTaskVl, null));
                        }
                        else if(type.equals("DATE")){
                            if(value.isNull()){
                                return new PropertyValue(id, property,  new DateValued(idTaskVl, null));
                            }
                            return new PropertyValue(id, property,  new DateValued(idTaskVl, LocalDateTime.parse(value.asText())));

                        }
                        else if(type.equals("NUMBER") || type.equals("PROGRESS")){
                            return new PropertyValue(id, property, new NumberValued(idTaskVl, value.asDouble()));
                        }
                        else if(type.equals("RADIO") || type.equals("SELECT")){
                            if(isPresent(value, "id")){
                                Long idOpt = value.get("id").asLong();
                                String name = value.get("name").asText();
                                return new PropertyValue(id, property,  new UniOptionValued(idTaskVl, new Option(idOpt, name)));
                            }
                            return new PropertyValue(id, property, new UniOptionValued(idTaskVl, null));
                        }
                        else if(type.equals("CHECKBOX") || type.equals("TAG")){
                            ArrayList<Option> options = new ArrayList<>();
                            for(JsonNode valueF : value){
                                if(isPresent(valueF, "id")){
                                    String name = valueF.get("name").asText();
                                    Long idOpt = valueF.get("id").asLong();
                                    options.add(new Option(idOpt, name));

                                }
                            }

                            return new PropertyValue(id, property, new MultiOptionValued(idTaskVl, options));
                        }
                        else if(type.equals("TIME")){
                            String color = value.get("color").asText();
                            ArrayList<LocalDateTime> starts = new ArrayList<>();
                            // I have merda here
                            Long idIntervals = value.get("id").asLong();
                                for(JsonNode valueF : value.get("starts")){
                                    starts.add(LocalDateTime.parse(valueF.asText()));
                                }
                            ArrayList<LocalDateTime> ends = new ArrayList<>();
                                for(JsonNode valueF : value.get("ends")){
                                    ends.add(LocalDateTime.parse(valueF.asText()));
                                }
                            JsonNode time = value.get("time");
                            if(time.isNull()){
                                return new PropertyValue(id, property, new TimeValued(idTaskVl, new Intervals(idIntervals, null, starts, ends, color)));
                            }
                            Long idTime = null;
                            if (!time.get("id").isNull()){
                                idTime = time.get("id").asLong();
                            }
                            Integer seconds = time.get("seconds").asInt();
                            Integer minutes = time.get("minutes").asInt();
                            Integer hours = time.get("hours").asInt();
                            return new PropertyValue(id, property, new TimeValued(idTaskVl,
                                    new Intervals(idIntervals, new Duration(idTime, seconds, minutes, hours), starts, ends, color)));
                        }

                        else if(type.equals("USER")){
                            ArrayList<User> users = new ArrayList<>();
                            for(JsonNode valueF : value){
                                if(isPresent(valueF, "id")){
                                    Long idUser = valueF.get("id").asLong();
                                    users.add(new User(idUser));
                                }
                            }
                            return new PropertyValue(id, property, new UserValued(idTaskVl, users));
                        }
                        throw new DeserializerException("Property have a unknown type");
                    }
                    throw new DeserializerException("Value object dont have a value or Id!");
                }
                throw new DeserializerException("TaskValue don't have a value");
            }
                throw new DeserializerException("Property don't have type attribute or id");
        }
            throw new DeserializerException("TaskValue don't have a property");
    }

    private boolean isPresent(JsonNode jsonNode, String text) {
        try {
            if (jsonNode.findParent(text) != null) {
                return true;
            } else {
                throw new NullPointerException();
            }
        } catch (NullPointerException e) {
            return false;
        }

    }
}