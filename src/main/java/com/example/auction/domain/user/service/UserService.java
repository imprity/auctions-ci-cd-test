package com.example.auction.domain.user.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auth.exception.AuthErrorEnum;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.repository.BidRepository;
import com.example.auction.domain.user.dto.UserChangePasswordRequest;
import com.example.auction.domain.user.dto.UserGetResponse;
import com.example.auction.domain.user.dto.UserWithdrawResponse;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    @Transactional(readOnly = true)
    public UserGetResponse myPage(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        return new UserGetResponse(
                user.getId(),
                user.getEmail(),
                user.getRating(),
                user.getRole()
        );
    }

    @Transactional
    public void changePassword(Long userId, UserChangePasswordRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        if (request.newPassword().equals(request.oldPassword())) {
            throw new ServiceErrorException(AuthErrorEnum.SAME_AS_OLD_PASSWORD);
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new ServiceErrorException(AuthErrorEnum.INVALID_PASSWORD);
        }

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedNewPassword);
    }

    @Transactional
    public UserWithdrawResponse withdraw(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        if (auctionRepository.existsByUserIdAndStatusIn(userId, List.of(AuctionStatus.READY, AuctionStatus.ACTIVE))) {
            throw new ServiceErrorException(UserErrorEnum.HAS_ACTIVE_AUCTION);
        }

        if (bidRepository.existsByUserIdAndStatus(userId, BidAuctionStatus.ACTIVE)) {
            throw new ServiceErrorException(UserErrorEnum.HAS_ACTIVE_BID);
        }

        user.delete();

        return new UserWithdrawResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }
}
