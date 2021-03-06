package dekk.pw.pokemate.tasks;

import POGOProtos.Inventory.Item.ItemIdOuterClass;
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass;
import com.pokegoapi.api.inventory.Item;
import com.pokegoapi.api.inventory.Pokeball;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.EncounterResult;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import dekk.pw.pokemate.Config;
import dekk.pw.pokemate.Context;
import dekk.pw.pokemate.PokeMateUI;
import dekk.pw.pokemate.Walking;

import java.util.Collections;
import java.util.List;

/**
 * Created by TimD on 7/21/2016.
 */
public class CatchPokemon implements Task {

    public void run(Context context) {
        try {
            Pokeball pokeball = null;
            List<CatchablePokemon> pokemon = context.getApi().getMap().getCatchablePokemon();
            if (pokemon.size() > 0) {
                Item ball = context.getApi().getInventories().getItemBag().getItem(ItemIdOuterClass.ItemId.forNumber(Config.getPreferredBall()));
                if (ball != null && ball.getCount() > 0) {
                    pokeball = getBall(Config.getPreferredBall());
                } else {
                    //find any pokeball we can.
                    for (Pokeball pb : Pokeball.values()) {
                        ball = context.getApi().getInventories().getItemBag().getItem(pb.getBallType());
                        if (ball != null && ball.getCount() > 0) {
                            pokeball = pb;
                            break;
                        }
                    }
                }
                CatchablePokemon target = pokemon.get(0);
                if (target != null && pokeball != null) {
                    Walking.setLocation(context);
                    EncounterResult encounterResult = target.encounterPokemon();
                    if (encounterResult.wasSuccessful()) {
                        CatchResult catchResult = target.catchPokemon(pokeball);
                        if (catchResult.getStatus().equals(CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus.CATCH_SUCCESS)) {
                            try {
                                List<Pokemon> pokemonList = context.getApi().getInventories().getPokebank().getPokemons();
                                Collections.sort(pokemonList, (a, b) -> Long.compare(a.getCreationTimeMs(), b.getCreationTimeMs()));
                                Pokemon p = pokemonList.get(pokemonList.size() - 1);
                                PokeMateUI.toast("Caught a " + getIvRatio(p) + "% " +  target.getPokemonId() + " [" +p.getIndividualAttack() + "/" + p.getIndividualDefense() + "/" + p.getIndividualStamina() + "] at " + p.getCp() + " CP using a " + ball.getItemId().name() + " (Candy: " + p.getCandy() + ")");
                            } catch (NullPointerException | IndexOutOfBoundsException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            System.out.println(target.getPokemonId() + " fled.");
                        }
                    }
                }
            }
        } catch (LoginFailedException | RemoteServerException e) {
            e.printStackTrace();
        }
    }

    public int getIvRatio(Pokemon pokemon) {
        return (pokemon.getIndividualAttack() + pokemon.getIndividualDefense() + pokemon.getIndividualStamina()) * 100 / 45;
    }

    private Pokeball getBall(int id) {
        switch (id) {
            case ItemIdOuterClass.ItemId.ITEM_GREAT_BALL_VALUE:
                return Pokeball.GREATBALL;
            case ItemIdOuterClass.ItemId.ITEM_ULTRA_BALL_VALUE:
                return Pokeball.ULTRABALL;
            case ItemIdOuterClass.ItemId.ITEM_MASTER_BALL_VALUE:
                return Pokeball.MASTERBALL;
            default:
                return Pokeball.POKEBALL;
        }
    }


}
