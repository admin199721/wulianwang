package org.jetlinks.community.device.measurements;

import org.jetlinks.community.dashboard.DashboardObject;
import org.jetlinks.community.dashboard.Measurement;
import org.jetlinks.community.dashboard.ObjectDefinition;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.metadata.DeviceMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DeviceDashboardObject implements DashboardObject {
    private final String id;

    private final String name;

    private final DeviceProductOperator productOperator;

    private final EventBus eventBus;

    private final DeviceDataService deviceDataService;

    private DeviceDashboardObject(String id, String name,
                                  DeviceProductOperator productOperator,
                                  EventBus eventBus,
                                  DeviceDataService dataService) {
        this.id = id;
        this.name = name;
        this.productOperator = productOperator;
        this.eventBus = eventBus;
        this.deviceDataService = dataService;
    }

    public static DeviceDashboardObject of(String id, String name,
                                                                                DeviceProductOperator productOperator,
                                                                                EventBus eventBus,
                                                                                DeviceDataService dataService ) {
        return new DeviceDashboardObject(id, name, productOperator, eventBus, dataService);
    }

    @Override
    public ObjectDefinition getDefinition() {
        return new ObjectDefinition() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    @Override
    public Flux<Measurement> getMeasurements() {
        return Flux.concat(

            productOperator.getMetadata()
                .flatMapIterable(DeviceMetadata::getEvents)
                .map(event -> new DeviceEventMeasurement(productOperator.getId(), eventBus, event, deviceDataService)),

            productOperator.getMetadata()
                .map(metadata -> new DevicePropertiesMeasurement(productOperator.getId(), eventBus, deviceDataService, metadata)),

            productOperator.getMetadata()
                .map(metadata -> new DeviceEventsMeasurement(productOperator.getId(), eventBus, metadata, deviceDataService)),

            productOperator.getMetadata()
                .flatMapIterable(DeviceMetadata::getProperties)
                .map(event -> new DevicePropertyMeasurement(productOperator.getId(), eventBus, event, deviceDataService))
        );
    }

    @Override
    public Mono<Measurement> getMeasurement(String id) {
        if ("properties".equals(id)) {
            return productOperator.getMetadata()
                .map(metadata -> new DevicePropertiesMeasurement(productOperator.getId(), eventBus, deviceDataService, metadata));
        }
        if ("events".equals(id)) {
            return productOperator.getMetadata()
                .map(metadata -> new DeviceEventsMeasurement(productOperator.getId(), eventBus, metadata, deviceDataService));
        }
        return productOperator.getMetadata()
            .flatMap(metadata -> Mono.justOrEmpty(metadata.getEvent(id)))
            .<Measurement>map(event -> new DeviceEventMeasurement(productOperator.getId(), eventBus, event, deviceDataService))
            //???????????????????????????????????????
            .switchIfEmpty(productOperator.getMetadata()
                .flatMap(metadata -> Mono.justOrEmpty(metadata.getProperty(id)))
                .map(event -> new DevicePropertyMeasurement(productOperator.getId(), eventBus, event, deviceDataService)));
    }
}
