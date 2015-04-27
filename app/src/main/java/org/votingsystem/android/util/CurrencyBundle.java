package org.votingsystem.android.util;

import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Payment;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyBundle {

    private BigDecimal amount;
    private BigDecimal wildTagAmount;
    private List<Currency> tagCurrencyList;
    private List<Currency> wildTagCurrencyList;
    private String currencyCode;
    private String tagVS;
    private Currency leftOverCurrency;

    public CurrencyBundle(BigDecimal amount, Collection<Currency> tagCurrencyList,
              String currencyCode, String tagVS) {
        this.amount = amount;
        this.tagCurrencyList = new ArrayList<>(tagCurrencyList);
        this.currencyCode = currencyCode;
        this.tagVS = tagVS;
    }

    public CurrencyBundle(BigDecimal amount, String currencyCode, List<Currency> tagCurrencyList,
                          String tag) {
        this.tagVS = tag;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.tagCurrencyList = tagCurrencyList;
        Collections.sort(this.tagCurrencyList, currencyComparator);
    }

    public Set<Currency> getCurrencySet() {
        Set<Currency> result = new HashSet<>(tagCurrencyList);
        if(wildTagCurrencyList != null) result.addAll(wildTagCurrencyList);
        return result;
    }

    public static CurrencyBundle load(Collection<Currency> currencyList) throws ExceptionVS {
        String tagVS = null;
        String currencyCode = null;
        BigDecimal amount = BigDecimal.ZERO;
        for(Currency currency : currencyList) {
            if(tagVS == null) tagVS = currency.getSignedTagVS();
            else if(!tagVS.equals(currency.getSignedTagVS())) throw new ExceptionVS("bundle with mixed" +
                    "tags: " + tagVS + ", " + currency.getSignedTagVS());
            if(currencyCode == null) currencyCode = currency.getCurrencyCode();
            else if(!currencyCode.equals(currency.getCurrencyCode())) throw new ExceptionVS(
                    "bundle with mixed curency codes : " + currencyCode + ", " + currency.getCurrencyCode());
            amount = amount.add(currency.getAmount());
        }
        return new CurrencyBundle(amount, currencyList, currencyCode, tagVS);
    }

    public List<Currency> getTagCurrencyList() {
        return tagCurrencyList;
    }

    public void setTagCurrencyList(List<Currency> tagCurrencyList) {
        this.tagCurrencyList = tagCurrencyList;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTagVS() {
        return tagVS;
    }

    public void setTagVS(String tagVS) {
        this.tagVS = tagVS;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public List<Currency> getWildTagCurrencyList() {
        return wildTagCurrencyList;
    }

    public void setWildTagCurrencyList(List<Currency> wildTagCurrencyList) {
        this.wildTagCurrencyList = wildTagCurrencyList;
    }

    public BigDecimal getWildTagAmount() {
        return wildTagAmount;
    }

    public void setWildTagAmount(BigDecimal wildTagAmount) {
        this.wildTagAmount = wildTagAmount;
    }

    public BigDecimal getTotalAmount() {
        if(amount != null) {
            if(wildTagAmount != null) return amount.add(wildTagAmount);
            else return amount;
        } else if(wildTagAmount != null) {
            return wildTagAmount;
        } else return BigDecimal.ZERO;
    }

    private Currency getLeftOverCurrency(BigDecimal requestAmount)
            throws Exception {
        BigDecimal bundleAmount = getTotalAmount();
        if(bundleAmount.compareTo(requestAmount) == 0) return null;
        else if(requestAmount.compareTo(bundleAmount) < 0) {
            return new Currency( AppContextVS.getInstance().getCurrencyServer().getServerURL(),
                    bundleAmount.subtract(requestAmount), currencyCode, tagVS);
        } else throw new ValidationExceptionVS(MessageFormat.format(
                "requestAmount ''{0}'' exceeds bundle amount ''{1}''", requestAmount, bundleAmount));

    }

    public Currency getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public CurrencyBatchDto getCurrencyBatchDto(TransactionVSDto transactionDto) throws Exception {
        return getCurrencyBatchDto(transactionDto.getOperation(), transactionDto.getPaymentMethod(),
                transactionDto.getSubject(), transactionDto.getToUserIBAN().get(0),
                transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                transactionDto.getTagVS().getName(), transactionDto.isTimeLimited(),
                AppContextVS.getInstance().getTimeStampServiceURL());
    }
    
    public CurrencyBatchDto getCurrencyBatchDto(TypeVS operation, Payment paymentMethod, String subject, 
            String toUserIBAN, BigDecimal batchAmount, String currencyCode, String tag, 
            Boolean isTimeLimited, String timeStampServiceURL) throws Exception {
        CurrencyBatchDto dto = new CurrencyBatchDto();
        dto.setOperation(operation);
        dto.setPaymentMethod(paymentMethod);
        dto.setSubject(subject);
        dto.setToUserIBAN(toUserIBAN);
        dto.setBatchAmount(batchAmount);
        dto.setCurrencyCode(currencyCode);
        dto.setTag(tag);
        dto.setBatchUUID(UUID.randomUUID().toString());
        Set<Currency> transactionCurrencySet = getCurrencySet();
        List<String> currencyTransactionBatch = new ArrayList<>();
        for (Currency currency : transactionCurrencySet) {
            SMIMEMessage smimeMessage = currency.getCertificationRequest().getSMIME(
                    currency.getHashCertVS(), StringUtils.getNormalized(currency.getToUserName()),
                    JSON.getMapper().writeValueAsString(dto), subject, null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, AppContextVS.getInstance());
            timeStamper.call();
            currency.setSmimeMessage(timeStamper.getSMIME());
            currencyTransactionBatch.add(new String(Base64.encode(currency.getSmimeMessage().getBytes())));
        }
        leftOverCurrency = getLeftOverCurrency(batchAmount);
        if (leftOverCurrency != null) dto.setCsrCurrency(new String(
                leftOverCurrency.getCertificationRequest().getCsrPEM(), "UTF-8"));
        dto.setCurrency(currencyTransactionBatch);
        return dto;
    }

    public void updateWallet(Currency leftOverCurrency, AppContextVS contextVS) throws Exception {
        Set<Currency> currencyListToRemove = getCurrencySet();
        Wallet.removeCurrencyCollection(currencyListToRemove, contextVS);
        if(leftOverCurrency != null) Wallet.updateWallet(
                new HashSet<Currency>(Arrays.asList(leftOverCurrency)),contextVS);
    }

    private static Comparator<Currency> currencyComparator = new Comparator<Currency>() {
        public int compare(Currency c1, Currency c2) {
            return c1.getAmount().compareTo(c2.getAmount());
        }
    };
}