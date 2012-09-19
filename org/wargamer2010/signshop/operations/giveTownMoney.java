package org.wargamer2010.signshop.operations;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.entity.Player;

import org.wargamer2010.signshop.Vault;
import org.wargamer2010.signshop.util.economyUtil;
import org.wargamer2010.signshop.util.signshopUtil;
import org.wargamer2010.signshop.player.SignShopPlayer;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class giveTownMoney implements SignShopOperation {
	@Override
	public Boolean setupOperation(SignShopArguments ssArgs) {
		ssArgs.setMessagePart("!price", economyUtil.formatMoney(ssArgs.get_fPrice()));
		return true;
	}

	@Override
	public Boolean checkRequirements(SignShopArguments ssArgs, Boolean activeCheck) {
            SignShopPlayer ssPlayer = ssArgs.get_ssPlayer();
            if (ssPlayer.getPlayer() == null)
                return true;
            
            ssArgs.set_fPrice(ssArgs.get_fPrice());
            ssArgs.setMessagePart("!price", economyUtil.formatMoney(ssArgs.get_fPrice()));

            try {
                Resident resident = TownyUniverse.getDataSource().getResident(ssPlayer.getName());
                Town town = resident.getTown();
                if (!resident.isMayor()) {
                    if (!town.hasAssistant(resident)) {
                        ssPlayer.sendMessage(signshopUtil.getError("towny_owner_not_mayor_or_assistant", ssArgs.messageParts));
                        return false;
                    }
                }
                if (Vault.economy == null) {
                    ssPlayer.sendMessage("Error with the economy, tell the System Administrator to install Vault properly.");
                    return false;
                } else if (town.getEconomyName().equals("")) {
                    ssPlayer.sendMessage(signshopUtil.getError("towny_owner_not_belong_to_town", ssArgs.messageParts));
                    return false;
                }
            } catch (TownyException x) {
                // TownyMessaging.sendErrorMsg(player, x.getMessage());
                ssPlayer.sendMessage(signshopUtil.getError("towny_owner_not_belong_to_town", ssArgs.messageParts));
                return false;
            }
            return true;
	}

	@Override
	public Boolean runOperation(SignShopArguments ssArgs) {
            SignShopPlayer ssPlayer = ssArgs.get_ssPlayer();
            if (ssPlayer == null) {
                    return false;
            }

            Float fPricemod = ssArgs.get_ssPlayer().getPlayerPricemod(ssArgs.get_sOperation(), true);
            Float fPrice = (ssArgs.get_fPrice() * fPricemod);

            // then deposit it into the bank
            Resident resident;
            Town town;
            try {
                resident = TownyUniverse.getDataSource().getResident(ssArgs.get_ssOwner().getName());
                town = resident.getTown();

                double bankcap = TownySettings.getTownBankCap();
                if (bankcap > 0) {
                    if (fPrice + town.getHoldingBalance() > bankcap)
                        throw new TownyException(String.format(TownySettings.getLangString("msg_err_deposit_capped"), bankcap));
                }

                EconomyResponse response = Vault.economy.depositPlayer(town.getEconomyName(), fPrice);
                if (response.type != EconomyResponse.ResponseType.SUCCESS) {
                    ssPlayer.sendMessage("Error depositing into shop owners account!");
                    return false;
                }

                return true;
            } catch (TownyException x) {
                TownyMessaging.sendErrorMsg(ssPlayer.getPlayer(), x.getMessage());
            } catch (EconomyException x) {
                TownyMessaging.sendErrorMsg(ssPlayer.getPlayer(), x.getMessage());
            }
            return false;
	}
}