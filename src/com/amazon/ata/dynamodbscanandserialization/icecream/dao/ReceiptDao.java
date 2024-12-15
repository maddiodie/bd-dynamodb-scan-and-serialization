package com.amazon.ata.dynamodbscanandserialization.icecream.dao;

import com.amazon.ata.dynamodbscanandserialization.icecream.converter.ZonedDateTimeConverter;
import com.amazon.ata.dynamodbscanandserialization.icecream.model.Receipt;
import com.amazon.ata.dynamodbscanandserialization.icecream.model.Sundae;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Provides access to receipts in the datastore.
 */
public class ReceiptDao {

    private final ZonedDateTimeConverter converter;
    private final DynamoDBMapper mapper;

    /**
     * Constructs a DAO with the given mapper.
     * @param mapper The DynamoDBMapper to use
     */
    @Inject
    public ReceiptDao(DynamoDBMapper mapper) {
        this.mapper = mapper;
        this.converter = new ZonedDateTimeConverter();
    }

    /**
     * Generates and persists a customer receipt. The salesTotal is the sum of the price of the
     * provided sundaes.
     * @param customerId - the id of the ordering customer
     * @param sundaeList - the sundaes ordered by the customer
     * @return the receipt stored in the database
     */
    public Receipt createCustomerReceipt(String customerId, List<Sundae> sundaeList) {
        Receipt receipt = new Receipt();
        receipt.setCustomerId(customerId);
        receipt.setPurchaseDate(ZonedDateTime.now());
        receipt.setSalesTotal(sundaeList.stream().map(Sundae::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        receipt.setSundaes(sundaeList);
        mapper.save(receipt);
        return receipt;
    }

    /**
     * Calculates the total sales for the time period between fromDate and toDate (inclusive).
     * @param fromDate - the date (inclusive of) to start tracking sales
     * @param toDate - the date (inclusive of) to stop tracking sales
     * @return the total values of sundae sales for the requested time period
     */
    public BigDecimal getSalesBetweenDates(ZonedDateTime fromDate, ZonedDateTime toDate) {
        Map<String, AttributeValue> valueMap = new HashMap<>();
        // (1)
        valueMap.put(":startDate", new AttributeValue().withS(converter.convert(fromDate)));
        valueMap.put(":endDate", new AttributeValue().withS(converter.convert(toDate)));
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("purchaseDate between :startDate and :endDate")
                .withExpressionAttributeValues(valueMap);
        // (2)

        PaginatedScanList<Receipt> result = mapper.scan(Receipt.class, scanExpression);

        return result.stream()
                .map(Receipt::getSalesTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // (3)
    }
    // plan
    // (1) create a map to hold the start and end date
    // (2) build a scan expression with a filter between the start and end date using the values from
    //     the map
    // (3) return the resulting stream, mapping over each sale to get the sales total, and reduce this
    //     to be a single big decimal value

    /**
     * Retrieves a subset of the receipts stored in the database. At least limit number of records
     * will be retrieved unless the end of the table has been reached, and instead only the remaining
     * records will be returned. An exclusive start key can be provided to start reading the table
     * from this record, but excluding it from results.
     * @param limit - the number of Receipts to return
     * @param exclusiveStartKey - an optional value provided to designate the start of the read
     * @return a list of Receipts
     */
    public List<Receipt> getReceiptsPaginated(int limit, Receipt exclusiveStartKey) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression().withLimit(limit);
        // (1)

        if (exclusiveStartKey != null) {
            Map<String, AttributeValue> startingKeyMap = new HashMap<>();
            startingKeyMap.put("customerId", new AttributeValue()
                    .withS(exclusiveStartKey.getCustomerId()));
            // (2)
            startingKeyMap.put("purchaseDate", new AttributeValue()
                    .withS(converter.convert(exclusiveStartKey.getPurchaseDate())));
            // (3)
            scanExpression.setExclusiveStartKey(startingKeyMap);
        }

        ScanResultPage<Receipt> receiptPage = mapper.scanPage(Receipt.class, scanExpression);
        // (4)

        return receiptPage.getResults();
        // (5)
    }
    // plan
    // (1) build a scan expression with limit
    // (2) as long as there is an exclusive starting key, then we can create a starting key map to
    //     hold the customer id and the purchase date
    // (3) set the scan expression exclusive starting key
    // (4) create a receipt page by using the scanPage() method from mapper passing in the class and
    //     scan expression
    // (5) return the results from the receipt page

}
