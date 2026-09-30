package com.walletapp.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 400 Bad Request
    INVALID_PHONE_FORMAT("INVALID_PHONE_FORMAT", HttpStatus.BAD_REQUEST, "Số điện thoại không đúng định dạng Việt Nam"),
    WEAK_PASSWORD("WEAK_PASSWORD", HttpStatus.BAD_REQUEST, "Mật khẩu tối thiểu 8 ký tự, gồm ít nhất 1 chữ hoa, 1 chữ thường và 1 số"),
    INVALID_PIN_FORMAT("INVALID_PIN_FORMAT", HttpStatus.BAD_REQUEST, "Mã PIN phải bao gồm đúng 6 chữ số"),
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ"),

    // 401 Unauthorized
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Yêu cầu xác thực tài khoản"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Số điện thoại hoặc mật khẩu không chính xác"),
    INVALID_PIN("INVALID_PIN", HttpStatus.UNAUTHORIZED, "Mã PIN không chính xác"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn"),
    INVALID_TOKEN("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Token xác thực không hợp lệ"),
    REFRESH_TOKEN_EXPIRED("REFRESH_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn"),
    DEVICE_NOT_REGISTERED("DEVICE_NOT_REGISTERED", HttpStatus.UNAUTHORIZED, "Thiết bị chưa được đăng ký"),

    // 403 Forbidden
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Không đủ quyền truy cập"),
    WALLET_FROZEN("WALLET_FROZEN", HttpStatus.FORBIDDEN, "Ví của bạn đang bị khóa, không thể giao dịch"),

    // 404 Not Found
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy thông tin người dùng"),
    WALLET_NOT_FOUND("WALLET_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy thông tin ví"),

    // 409 Conflict
    PHONE_ALREADY_EXISTS("PHONE_ALREADY_EXISTS", HttpStatus.CONFLICT, "Số điện thoại đã được đăng ký trong hệ thống"),
    DUPLICATE_REQUEST("DUPLICATE_REQUEST", HttpStatus.CONFLICT, "Giao dịch đang được xử lý, vui lòng không gửi lại"),
    CONCURRENCY_CONFLICT("CONCURRENCY_CONFLICT", HttpStatus.CONFLICT, "Hệ thống đang bận xử lý ví, vui lòng thử lại sau giây lát"),

    // 410 Gone
    QR_EXPIRED("QR_EXPIRED", HttpStatus.GONE, "Mã QR đã hết hạn sử dụng"),
    QR_ALREADY_USED("QR_ALREADY_USED", HttpStatus.GONE, "Mã QR động đã được thanh toán trước đó"),

    // 422 Unprocessable Entity
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", HttpStatus.UNPROCESSABLE_ENTITY, "Số dư ví không đủ để thực hiện giao dịch"),
    BELOW_MINIMUM_AMOUNT("BELOW_MINIMUM_AMOUNT", HttpStatus.UNPROCESSABLE_ENTITY, "Số tiền giao dịch dưới mức tối thiểu"),
    EXCEEDS_PER_TRANSACTION_LIMIT("EXCEEDS_PER_TRANSACTION_LIMIT", HttpStatus.UNPROCESSABLE_ENTITY, "Số tiền vượt quá hạn mức cho mỗi giao dịch"),
    EXCEEDS_DAILY_LIMIT("EXCEEDS_DAILY_LIMIT", HttpStatus.UNPROCESSABLE_ENTITY, "Tổng số tiền vượt quá hạn mức giao dịch trong ngày"),
    DEST_WALLET_EXCEEDS_MAX_BALANCE("DEST_WALLET_EXCEEDS_MAX_BALANCE", HttpStatus.UNPROCESSABLE_ENTITY, "Ví người nhận vượt trần số dư tối đa cho phép"),
    SELF_TRANSFER_NOT_ALLOWED("SELF_TRANSFER_NOT_ALLOWED", HttpStatus.UNPROCESSABLE_ENTITY, "Không thể tự chuyển tiền cho chính ví của mình"),
    IDEMPOTENCY_PAYLOAD_MISMATCH("IDEMPOTENCY_PAYLOAD_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency key đã được dùng với nội dung giao dịch khác"),
    QR_INVALID_CHECKSUM("QR_INVALID_CHECKSUM", HttpStatus.UNPROCESSABLE_ENTITY, "Mã QR không hợp lệ hoặc đã bị thay đổi (sai checksum)"),

    // 423 Locked
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", HttpStatus.LOCKED, "Tài khoản hoặc ví đang bị tạm khóa"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống nội bộ, vui lòng thử lại sau");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
