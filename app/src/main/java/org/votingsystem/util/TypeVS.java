package org.votingsystem.util;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum TypeVS {

    ACCESS_REQUEST,
    ITEM_REQUEST,
    ITEMS_REQUEST,
    VOTING_EVENT,
    CANCEL_VOTE,

    REPRESENTATIVE_REVOKE,
    NEW_REPRESENTATIVE,
    REPRESENTATIVE,
    ANONYMOUS_REPRESENTATIVE_SELECTION,
    ANONYMOUS_SELECTION_CERT_REQUEST,
    ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION,
    REPRESENTATIVE_SELECTION,

    PIN,
    PIN_CHANGE,
    EVENT_CANCELLATION,
    BACKUP_REQUEST,
    SEND_VOTE,
    VOTING_PUBLISHING,

    CURRENCY,
    CURRENCY_CANCEL,
    CURRENCY_CHECK,
    FROM_USERVS,
    CURRENCY_GROUP_NEW,
    CURRENCY_PERIOD_INIT,
    CURRENCY_REQUEST,
    CURRENCY_CHANGE,
    CURRENCY_WALLET_CHANGE,
    CURRENCY_SEND,
    CURRENCY_ACCOUNTS_INFO,
    DEVICE_SELECT,
    TRANSACTIONVS_INFO,
    TRANSACTIONVS_RESPONSE,
    CURRENCY_BATCH,

    FROM_BANKVS,
    FROM_GROUP_TO_MEMBER_GROUP,
    FROM_GROUP_TO_MEMBER,
    FROM_GROUP_TO_ALL_MEMBERS,
    STATE,
    OPERATION_CANCELED,
    OPERATION_FINISHED,

    QR_MESSAGE_INFO,

    DELIVERY_WITHOUT_PAYMENT,
    DELIVERY_WITH_PAYMENT,
    REQUEST_FORM,

    MESSAGEVS,
    MESSAGEVS_SIGN,
    MESSAGEVS_SIGN_RESPONSE,
    MESSAGEVS_TO_DEVICE,
    MESSAGEVS_FROM_DEVICE,
    MESSAGEVS_FROM_VS,

    LISTEN_TRANSACTIONS,
    INIT_SIGNED_SESSION,
    WEB_SOCKET_INIT,
    WEB_SOCKET_CLOSE,
    WEB_SOCKET_BAN_SESSION,
    RECEIPT;

}
