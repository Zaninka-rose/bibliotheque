package com.ibizabroker.bibliotheque.entity;

import org.springframework.stereotype.Component;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class JsonDataSerializer extends ValueSerializer<Date> {

    @Override
    public void serialize(Date date, JsonGenerator gen, SerializationContext serializers) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        gen.writeString(simpleDateFormat.format(date));
    }

}
