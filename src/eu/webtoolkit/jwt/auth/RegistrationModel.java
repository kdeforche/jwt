/*
 * Copyright (C) 2020 Emweb bv, Herent, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.auth;

import eu.webtoolkit.jwt.*;
import eu.webtoolkit.jwt.auth.mfa.*;
import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.servlet.*;
import eu.webtoolkit.jwt.utils.*;
import java.io.*;
import java.lang.ref.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model for implementing a registration view.
 *
 * <p>This model implements the logic for the registration of a new user. It can deal with
 * traditional username/password registration, or registration of pre-identified users using
 * federated login.
 *
 * <p>The model exposes four fields:
 *
 * <ul>
 *   <li>LoginNameField: the login name (used as an identity for the {@link Identity#LoginName}
 *       provider) &ndash; this can be an email if the {@link AuthService} is configured to use
 *       email addresses as identity
 *   <li>ChoosePasswordField: the password
 *   <li>RepeatPasswordField: the password (repeated)
 *   <li>EmailField: if an email address is to be asked (and is not used as identity).
 * </ul>
 *
 * <p>The largest complexity is in the handling of third party identity providers, which is
 * initiated with a call to {@link RegistrationModel#registerIdentified(Identity identity)
 * registerIdentified()}.
 *
 * <p>When a user is re-identified with the same identity, then the model may require that the
 * (original) user confirms this new identity. The model indicates that this button should be made
 * visible with {@link RegistrationModel#isConfirmUserButtonVisible() isConfirmUserButtonVisible()},
 * the action to take is determined by {@link RegistrationModel#getConfirmIsExistingUser()
 * getConfirmIsExistingUser()}, and {@link RegistrationModel#existingUserConfirmed()
 * existingUserConfirmed()} ends this process by merging the new identity into the existing user.
 *
 * <p>
 *
 * @see RegistrationWidget
 */
public class RegistrationModel extends FormBaseModel {
  private static Logger logger = LoggerFactory.getLogger(RegistrationModel.class);

  /** Choose Password field. */
  public static final String ChoosePasswordField = "choose-password";
  /** Repeat password field. */
  public static final String RepeatPasswordField = "repeat-password";
  /** Email field (if login name is not email) */
  public static final String EmailField = "email";
  /**
   * Constructor.
   *
   * <p>Creates a new registration model, using a basic authentication service and user database.
   *
   * <p>The <code>login</code> object is used to indicate that an existing user was re-identified,
   * and thus the registration process may be aborted.
   */
  public RegistrationModel(
      final AuthService baseAuth, final AbstractUserDatabase users, final Login login) {
    super(baseAuth, users);
    this.login_ = login;
    this.minLoginNameLength_ = 4;
    this.emailPolicy_ = EmailPolicy.Disabled;
    this.idpIdentity_ = new Identity();
    this.existingUser_ = new User();
    if (baseAuth.getIdentityPolicy() != IdentityPolicy.EmailAddress) {
      if (baseAuth.isEmailVerificationRequired()) {
        this.emailPolicy_ = EmailPolicy.Mandatory;
      } else {
        if (baseAuth.isEmailVerificationEnabled()) {
          this.emailPolicy_ = EmailPolicy.Optional;
        } else {
          this.emailPolicy_ = EmailPolicy.Disabled;
        }
      }
    }
    this.reset();
  }
  /**
   * Resets the model.
   *
   * <p>This resets the model to initial values, clearing any entered information (login name,
   * password, pre-identified identity).
   */
  public void reset() {
    this.idpIdentity_ = new Identity();
    this.existingUser_ = new User();
    if (this.getBaseAuth().getIdentityPolicy() == IdentityPolicy.EmailAddress) {
      this.addField(LoginNameField, WString.tr("Wt.Auth.email-info"));
    } else {
      this.addField(LoginNameField, WString.tr("Wt.Auth.user-name-info"));
    }
    this.addField(ChoosePasswordField, WString.tr("Wt.Auth.choose-password-info"));
    this.addField(RepeatPasswordField, WString.tr("Wt.Auth.repeat-password-info"));
    this.setEmailPolicy(this.emailPolicy_);
  }
  /** Returns the login object. */
  public Login getLogin() {
    return this.login_;
  }
  /**
   * Configures a minimum length for a login name.
   *
   * <p>The default value is 4.
   */
  public void setMinLoginNameLength(int chars) {
    this.minLoginNameLength_ = chars;
  }
  /**
   * Returns the minimum length for a login name.
   *
   * <p>
   *
   * @see RegistrationModel#setMinLoginNameLength(int chars)
   */
  public int getMinLoginNameLength() {
    return this.minLoginNameLength_;
  }
  /**
   * Configures whether an email address needs to be entered.
   *
   * <p>You may specify whether you want the user to enter an email address.
   *
   * <p>This has no effect when the IdentityPolicy is {@link IdentityPolicy#EmailAddress}.
   *
   * <p>The default policy is:
   *
   * <ul>
   *   <li>EmailOptional when email address verification is enabled
   *   <li>EmailDisabled otherwise
   * </ul>
   */
  public void setEmailPolicy(EmailPolicy policy) {
    this.emailPolicy_ = policy;
    switch (this.emailPolicy_) {
      case Mandatory:
        this.addField(EmailField, WString.tr("Wt.Auth.email-info"));
        break;
      case Optional:
        this.addField(EmailField, WString.tr("Wt.Auth.optional-email-info"));
        break;
      default:
        break;
    }
  }
  /**
   * Returns the email policy.
   *
   * <p>
   *
   * @see RegistrationModel#setEmailPolicy(EmailPolicy policy)
   */
  public EmailPolicy getEmailPolicy() {
    return this.emailPolicy_;
  }
  /**
   * Register a user authenticated by an identity provider.
   *
   * <p>Using a 3rd party authentication service such as OAuth, a user may be identified which is
   * not yet registered with the web application.
   *
   * <p>Then, you may still need to allow the user to complete registration, but because the user
   * already is identified and authenticated, this simplifies the registration form, since fields
   * related to authentication can be dropped.
   *
   * <p>Returns <code>true</code> if the given identity was already registered, and has been logged
   * in.
   */
  public boolean registerIdentified(final Identity identity) {
    this.idpIdentity_ = identity;
    if (this.idpIdentity_.isValid()) {
      User user = this.getBaseAuth().identifyUser(this.idpIdentity_, this.getUsers());
      if (user.isValid()) {
        return this.loginUser(this.login_, user);
      } else {
        switch (this.getBaseAuth().getIdentityPolicy()) {
          case LoginName:
            if (this.idpIdentity_.getName().length() != 0) {
              this.setValue(LoginNameField, this.idpIdentity_.getName());
            } else {
              if (this.idpIdentity_.getEmail().length() != 0) {
                String suggested = this.idpIdentity_.getEmail();
                int i = suggested.indexOf('@');
                if (i != -1) {
                  suggested = suggested.substring(0, 0 + i);
                }
                this.setValue(LoginNameField, new WString(suggested));
              }
            }
            break;
          case EmailAddress:
            if (this.idpIdentity_.getEmail().length() != 0) {
              this.setValue(LoginNameField, new WString(this.idpIdentity_.getEmail()));
            }
            break;
          default:
            break;
        }
        if (this.idpIdentity_.getEmail().length() != 0) {
          this.setValue(EmailField, this.idpIdentity_.getEmail());
          this.setValidation(
              EmailField, new WValidator.Result(ValidationState.Valid, WString.Empty));
        }
        return false;
      }
    } else {
      return false;
    }
  }
  /**
   * Returns the existing user that needs to be confirmed.
   *
   * <p>When a user wishes to register with an identity that corresponds to an existing user, he may
   * be allowd to confirm that he is in fact this existing user.
   *
   * <p>
   *
   * @see RegistrationModel#getConfirmIsExistingUser()
   */
  public User getExistingUser() {
    return this.existingUser_;
  }
  /**
   * Returns the method to be used to confirm to be an existing user.
   *
   * <p>When the ConfirmExisting field is visible, this returns an appropriate method to use to let
   * the user confirm that he is indeed the identified existing user.
   *
   * <p>The outcome of this method (if it is an online method, like a password prompt), if
   * successful, should be indicated using {@link RegistrationModel#existingUserConfirmed()
   * existingUserConfirmed()}.
   *
   * <p>
   *
   * @see RegistrationModel#existingUserConfirmed()
   */
  public IdentityConfirmationMethod getConfirmIsExistingUser() {
    if (this.existingUser_.isValid()) {
      if (!this.existingUser_.getPassword().isEmpty()) {
        return IdentityConfirmationMethod.ConfirmWithPassword;
      } else {
        if (this.getBaseAuth().isEmailVerificationEnabled()
            && this.existingUser_.getEmail().length() != 0) {
          return IdentityConfirmationMethod.ConfirmWithEmail;
        }
      }
    }
    return IdentityConfirmationMethod.ConfirmationNotPossible;
  }
  /**
   * Confirms that the user is indeed an existing user.
   *
   * <p>The new identity is added to this existing user (if applicable), and the user is logged in.
   */
  public void existingUserConfirmed() {
    if (this.idpIdentity_.isValid()) {
      this.existingUser_.addIdentity(this.idpIdentity_.getProvider(), this.idpIdentity_.getId());
    }
    this.loginUser(this.login_, this.existingUser_);
  }
  /**
   * Validates the login name.
   *
   * <p>This verifies that the login name is adequate (see also {@link
   * RegistrationModel#setMinLoginNameLength(int chars) setMinLoginNameLength()}).
   */
  public WString validateLoginName(final String userName) {
    switch (this.getBaseAuth().getIdentityPolicy()) {
      case LoginName:
        if ((int) userName.length() < this.minLoginNameLength_) {
          return WString.tr("Wt.Auth.user-name-tooshort").arg(this.minLoginNameLength_);
        } else {
          return WString.Empty;
        }
      case EmailAddress:
        if ((int) userName.length() < 3 || userName.indexOf('@') == -1) {
          return WString.tr("Wt.Auth.email-invalid");
        } else {
          return WString.Empty;
        }
      default:
        return WString.Empty;
    }
  }
  /**
   * Verifies that a user with that name does not yet exist.
   *
   * <p>If a user with that name already exists, it may in fact be the same user that is trying to
   * register again (perhaps using a different identification method). If possible, we allow the
   * user to confirm his identity.
   */
  public void checkUserExists(final String userName) {
    this.existingUser_ = this.getUsers().findWithIdentity(Identity.LoginName, userName);
  }
  /** Performs the registration process. */
  public User doRegister() {
    try {
      if (!(this.getPasswordAuth() != null) && !this.idpIdentity_.isValid()) {
        return new User();
      } else {
        User user = this.getUsers().registerNew();
        if (this.idpIdentity_.isValid()) {
          user.addIdentity(this.idpIdentity_.getProvider(), this.idpIdentity_.getId());
          if (this.getBaseAuth().getIdentityPolicy() != IdentityPolicy.Optional) {
            user.addIdentity(Identity.LoginName, this.valueText(LoginNameField));
          }
          String email = "";
          boolean emailVerified = false;
          if (this.idpIdentity_.getEmail().length() != 0) {
            email = this.idpIdentity_.getEmail();
            emailVerified = this.idpIdentity_.isEmailVerified();
          } else {
            if (this.getBaseAuth().getIdentityPolicy() == IdentityPolicy.EmailAddress) {
              email = this.valueText(LoginNameField);
            } else {
              email = this.valueText(EmailField);
            }
          }
          if (email.length() != 0) {
            if (emailVerified || !this.getBaseAuth().isEmailVerificationEnabled()) {
              user.setEmail(email);
            } else {
              this.getBaseAuth().verifyEmailAddress(user, email);
            }
          }
        } else {
          user.addIdentity(Identity.LoginName, this.valueText(LoginNameField));
          this.getPasswordAuth().updatePassword(user, this.valueText(ChoosePasswordField));
          if (this.getBaseAuth().isEmailVerificationEnabled()) {
            String email = "";
            if (this.getBaseAuth().getIdentityPolicy() == IdentityPolicy.EmailAddress) {
              email = this.valueText(LoginNameField);
            } else {
              email = this.valueText(EmailField);
            }
            if (email.length() != 0) {
              this.getBaseAuth().verifyEmailAddress(user, email);
            }
          }
        }
        return user;
      }
    } catch (Exception e) {
      logger.info("Ignoring exception {}", e.getMessage(), e);
      return null;
    }
  }

  public boolean isVisible(String field) {
    if (field == LoginNameField) {
      if (this.getBaseAuth().getIdentityPolicy() == IdentityPolicy.Optional) {
        return this.getPasswordAuth() != null && !this.idpIdentity_.isValid();
      } else {
        return true;
      }
    } else {
      if (field == ChoosePasswordField || field == RepeatPasswordField) {
        return this.getPasswordAuth() != null && !this.idpIdentity_.isValid();
      } else {
        if (field == EmailField) {
          if (this.getBaseAuth().getIdentityPolicy() == IdentityPolicy.EmailAddress) {
            return false;
          } else {
            if (this.emailPolicy_ == EmailPolicy.Disabled) {
              return false;
            } else {
              return true;
            }
          }
        } else {
          return true;
        }
      }
    }
  }

  public boolean isReadOnly(String field) {
    if (super.isReadOnly(field)) {
      return true;
    }
    if (field == LoginNameField) {
      return this.getBaseAuth().getIdentityPolicy() == IdentityPolicy.EmailAddress
          && this.idpIdentity_.isValid()
          && this.idpIdentity_.isEmailVerified();
    } else {
      if (field == EmailField) {
        return this.idpIdentity_.isValid() && this.idpIdentity_.isEmailVerified();
      } else {
        return false;
      }
    }
  }

  public boolean validateField(String field) {
    if (!this.isVisible(field)) {
      return true;
    }
    boolean valid = true;
    WString error = new WString();
    if (field == LoginNameField) {
      error = this.validateLoginName(this.valueText(field));
      if ((error.length() == 0)) {
        this.checkUserExists(this.valueText(field));
        boolean exists = this.existingUser_.isValid();
        valid = !exists;
        if (exists
            && this.getConfirmIsExistingUser()
                == IdentityConfirmationMethod.ConfirmationNotPossible) {
          error = WString.tr("Wt.Auth.user-name-exists");
        }
      } else {
        valid = false;
      }
      if (this.isReadOnly(field)) {
        valid = true;
      }
    } else {
      if (field == ChoosePasswordField) {
        AbstractPasswordService.AbstractStrengthValidator v =
            this.getPasswordAuth().getStrengthValidator();
        if (v != null) {
          WValidator.Result r =
              v.validate(
                  this.valueText(ChoosePasswordField),
                  this.valueText(LoginNameField),
                  this.valueText(EmailField));
          valid = r.getState() == ValidationState.Valid;
          error = r.getMessage();
        } else {
          valid = true;
        }
      } else {
        if (field == RepeatPasswordField) {
          if (this.getValidation(ChoosePasswordField).getState() == ValidationState.Valid) {
            if (!this.valueText(ChoosePasswordField).equals(this.valueText(RepeatPasswordField))) {
              error = WString.tr("Wt.Auth.passwords-dont-match");
            }
            valid = (error.length() == 0);
          } else {
            return true;
          }
        } else {
          if (field == EmailField) {
            String email = this.valueText(EmailField);
            if (email.length() != 0) {
              if ((int) email.length() < 3 || email.indexOf('@') == -1) {
                error = WString.tr("Wt.Auth.email-invalid");
              }
              if ((error.length() == 0)) {
                User user = this.getUsers().findWithEmail(email);
                if (user.isValid()) {
                  error = WString.tr("Wt.Auth.email-exists");
                }
              }
            } else {
              if (this.emailPolicy_ != EmailPolicy.Optional) {
                error = WString.tr("Wt.Auth.email-invalid");
              }
            }
            valid = (error.length() == 0);
          } else {
            return true;
          }
        }
      }
    }
    if (valid) {
      this.setValid(field, error);
    } else {
      this.setValidation(field, new WValidator.Result(ValidationState.Invalid, error));
    }
    return this.getValidation(field).getState() == ValidationState.Valid;
  }
  /**
   * Returns whether an existing user needs to be confirmed.
   *
   * <p>This returns whether the user is being identified as an existing user and he can confirm
   * that he is in fact the same user.
   */
  public boolean isConfirmUserButtonVisible() {
    return this.getConfirmIsExistingUser() != IdentityConfirmationMethod.ConfirmationNotPossible;
  }
  /**
   * Returns whether federated login options can be shown.
   *
   * <p>This returns whether fields for federated login (such as OAuth) should be shown. These are
   * typically buttons corresponding to identity providers.
   *
   * <p>The result of a federated authentication procedure should be indicated to {@link
   * RegistrationModel#registerIdentified(Identity identity) registerIdentified()}.
   */
  public boolean isFederatedLoginVisible() {
    return !this.getOAuth().isEmpty() && !this.idpIdentity_.isValid();
  }

  public static void validatePasswordsMatchJS(
      WLineEdit password, WLineEdit password2, WText info2) {
    password2
        .keyWentUp()
        .addListener(
            "function(o) {var i="
                + info2.getJsRef()
                + ",o1="
                + password.getJsRef()
                + ";if (!o1.classList.contains('Wt-invalid')) {if (o.value == o1.value) {o.classList.remove('Wt-invalid');Wt4.11.2.setHtml(i,"
                + WString.toWString(WString.tr("Wt.Auth.valid")).getJsStringLiteral()
                + ");} else {o.classList.remove('Wt-valid');Wt4.11.2.setHtml(i,"
                + WString.toWString(WString.tr("Wt.Auth.repeat-password-info")).getJsStringLiteral()
                + ");}}}");
  }

  private final Login login_;
  private int minLoginNameLength_;
  private EmailPolicy emailPolicy_;
  private Identity idpIdentity_;
  private User existingUser_;
}
