package edu.gatech.cs2110.circuitsim.api;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ra4king.circuitsim.gui.CircuitBoard;
import com.ra4king.circuitsim.gui.CircuitSim;
import com.ra4king.circuitsim.gui.peers.memory.RegisterPeer;
import com.ra4king.circuitsim.gui.peers.wiring.PinPeer;
import com.ra4king.circuitsim.gui.peers.wiring.PinPeer;
import com.ra4king.circuitsim.gui.Properties;
import com.ra4king.circuitsim.gui.Properties;
import com.ra4king.circuitsim.simulator.Circuit;
import com.ra4king.circuitsim.simulator.CircuitState;
import com.ra4king.circuitsim.simulator.components.memory.Register;
import com.ra4king.circuitsim.simulator.components.wiring.Pin;
import com.ra4king.circuitsim.simulator.Port;
import com.ra4king.circuitsim.simulator.Simulator;

/**
 * Represents and wraps the subcircuit to test.
 * <p>
 * Holds a CircuitSim {@code CircuitBoard} (also known as a
 * "subcircuit") and the {@code CircuitSim} instance used to simulate
 * it. Your tests shouldn't need to touch this, since it mainly provides
 * methods for loading a subcircuit from a file and poking at its
 * internal state to find components to test — all things {@link
 * edu.gatech.cs2110.circuitsim.extension.CircuitSimExtension} handles
 * for you.
 */
public class Subcircuit {
    private CircuitSim circuitSim;
    private CircuitBoard circuitBoard;

    private Subcircuit(CircuitSim circuitSim, CircuitBoard circuitBoard) {
        this.circuitSim = circuitSim;
        this.circuitBoard = circuitBoard;
    }

    /**
     * Returns the {@code CircuitSim} instance simulating the circuit.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code CircuitSim} instance used for simulation
     */
    public CircuitSim getCircuitSim() {
        return circuitSim;
    }

    /**
     * Returns the CircuitSim {@code Simulator} running the circuit.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code Simulator} instance used for simulation
     */
    public Simulator getSimulator() {
        return getCircuitSim().getSimulator();
    }

    /**
     * Returns the CircuitSim {@code CircuitBoard} for this subcircuit.
     * This is a GUI-side thing.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code CircuitBoard} of this subcircuit
     */
    public CircuitBoard getCircuitBoard() {
        return circuitBoard;
    }

    /**
     * Returns the CircuitSim {@code Circuit} for this subcircuit.
     * This is a simulation-side thing.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code Circuit} of this subcircuit
     */
    public Circuit getCircuit() {
        return circuitBoard.getCircuit();
    }

    /**
     * Returns the CircuitSim {@code CircuitState} of this subcircuit.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code CircuitState} of top level state of this subcircuit
     */
    public CircuitState getCircuitState() {
        return getCircuit().getTopLevelState();
    }

    // Don't hate me for `throws Exception', Roi made me do it
    /**
     * Loads the subcircuit named {@code subcircuitName} from the {@code
     * .sim} file {@code simFilePath}.
     * <p>
     * Note that for the sake of students' stress levels, {@code
     * subcircuitName} is normalized to a lowercase alphanumeric string
     * before searching the {@code .sim} file for it, as are the names of
     * subcircuits in the file. So {@code "1-bit adder!"} will match
     * {@code "1 Bit Adder"}, {@code "1bit adder"}, {@code "1bitadder"},
     * {@code "1 B  I T A D DD E R"}, and so on.
     *
     * @param  simFilePath    path to the subcircuit. Usually relative, like
     * {@code "adder.sim"}
     * @param  subcircuitName name of the subcircuit. Normalized as
     *                        described above before lookup.
     * @return a new {@code Subcircuit} simulating the requested
     *         subcircuit
     * @throws Exception      specified by {@code CircuitSim.loadCircuits()}
                              in violation of all good taste on Earth
     */
    public static Subcircuit fromPath(String simFilePath, String subcircuitName) throws Exception {
        File circuitFile = new File(simFilePath);

        // In theory, zucchini should guarantee this for us, but stuff
        // happens
        // TODO: Race condition R I P
        if (!circuitFile.canRead()) {
            throw new IllegalArgumentException(
                String.format("Cannot read circuit file `%s'", simFilePath));
        }

        CircuitSim circuitSim = new CircuitSim(false);
        circuitSim.loadCircuits(circuitFile);

        CircuitBoard circuitBoard = lookupSubcircuit(circuitSim, subcircuitName);

        return new Subcircuit(circuitSim, circuitBoard);
    }

    private static String canonicalName(String name) {
        return name.toLowerCase().replaceAll("[^0-9a-z]+", "");
    }

    private static CircuitBoard lookupSubcircuit(CircuitSim circuitSim, String subcircuitName) {
        String canonicalSubcircuitName = canonicalName(subcircuitName);
        Map<String, CircuitBoard> boards = circuitSim.getCircuitBoards();
        List<CircuitBoard> matchingCircuits = boards.entrySet().stream()
            .filter(entry -> canonicalName(entry.getKey()).equals(canonicalSubcircuitName))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

        if (matchingCircuits.size() == 0) {
            throw new IllegalArgumentException(String.format(
                "No subcircuits match the name `%s'. " +
                "Please double-check the names of all your subcircuits.",
                subcircuitName));
        } else if (matchingCircuits.size() > 1) {
            throw new IllegalArgumentException(String.format(
                "More than one subcircuit has the name `%s'. " +
                "Can't continue deterministically.",
                subcircuitName));
        }

        return matchingCircuits.get(0);
    }

    /**
     * Resets the simulation for this subcircuit.
     * <p>
     * The same thing as clicking Simulation → Reset Simulation in
     * CircuitSim.
     */
    public void resetSimulation() {
        circuitSim.getSimulator().reset();
    }

    /**
     * Finds a Pin component labelled {@code pinLabel} in this
     * subcircuit and returns a wrapper around it.
     * <p>
     * To make sure the tester is deterministic, this method requires
     * the subcircuit contain exactly one pin with a matching label.
     * The algorithm checks this before verifying if a match has the
     * right bit size or direction (input/output).
     * <p>
     * Also for the sake of students' stress levels, {@code pinLabel} is
     * normalized as described for {@link #lookupSubcircuit(CircuitSim,String)}
     * before lookup.
     *
     * @param  pinLabel     the label of the pin
     * @param  wantInputPin whether the pin found should be input or output
     * @param  wantBits     how many bits the pin found should have
     * @return either an {@link InputPin} or {@link OutputPin} depending
     *         on {@code wantInputPin}
     * @throws IllegalArgumentException if the subcircuit does not
     *                                  contain exactly one matching pin
     * @see InputPin
     * @see OutputPin
     */
    public BasePin lookupPin(String pinLabel, boolean wantInputPin, int wantBits) {
        String canonicalPinLabel = canonicalName(pinLabel);
        List<PinPeer> matchingPins = circuitBoard.getComponents().stream()
            .filter(component -> component instanceof PinPeer &&
                    ((PinPeer) component).getProperties().containsProperty(Properties.LABEL) &&
                    canonicalPinLabel.equals(
                        canonicalName(((PinPeer) component).getProperties().getValue(Properties.LABEL))))
            .map(component -> (PinPeer) component)
            .collect(Collectors.toList());

        if (matchingPins.size() > 1) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s' contains %d input/output pins labelled `%s', expected 1",
                circuitBoard.getCircuit().getName(), matchingPins.size(), canonicalPinLabel));
        } else if (matchingPins.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s' contains no input/output pins labelled `%s'!",
                circuitBoard.getCircuit().getName(), canonicalPinLabel));
        }

        PinPeer matchingPin = matchingPins.get(0);
        // Use their labels for error messages to be less confusing
        String theirPinLabel = matchingPin.getProperties().getValue(Properties.LABEL);

        if (matchingPin.isInput() != wantInputPin) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s' has %s pin labelled `%s', but expected it to be an %s pin instead",
                circuitBoard.getCircuit().getName(),
                theirPinLabel,
                matchingPin.isInput()? "input" : "output",
                wantInputPin? "input" : "output"));
        }

        int actualBits = matchingPin.getComponent().getBitSize();
        if (actualBits != wantBits) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s' has pin labelled `%s' with %d bits, but expected %d bits",
                circuitBoard.getCircuit().getName(),
                theirPinLabel, actualBits, wantBits));
        }

        BasePin pinWrapper = wantInputPin? new InputPin(matchingPin.getComponent(), this)
                                         : new OutputPin(matchingPin.getComponent(), this);
        return pinWrapper;
    }

    /**
     * Mocks the only register in this subcircuit by replacing it with
     * input and output pins. Useful for testing the combinational logic
     * in a sequential circuit with exactly one register. For more
     * details on the motivation behind {@link MockRegister}s, see
     * {@link MockRegister}.
     * <p>
     * Each register port will be disconnected from anything else and
     * replaced with a new Pin component reconnected to everything like
     * before.
     * <p>
     * Will blow up if this subcircuit does not contain exactly one
     * Register component. Often, this is what you want for state
     * machine subcircuits for example, and it does not require students
     * to label the register some arbitrary name buried deep in the
     * assignment PDF.
     *
     * @param  wantBits the bitsize of the register
     * @return a {@code MockRegister} which effectively acts a
     *         collection of input and output pins
     * @see    MockRegister
     */
    public MockRegister mockOnlyRegister(int wantBits) {
        List<Register> registers = circuitBoard.getComponents().stream()
            .filter(component -> component instanceof RegisterPeer)
            .map(component -> ((RegisterPeer) component).getComponent())
            .collect(Collectors.toList());

        if (registers.size() > 1) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s' contains %d registers, expected 1",
                circuitBoard.getCircuit().getName(), registers.size()));
        } else if (registers.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s' contains no registers!",
                circuitBoard.getCircuit().getName()));
        }

        Register reg = registers.get(0);

        int actualBits = reg.getBitSize();
        if (actualBits != wantBits) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s' has register with %d bits, but expected %d bits",
                circuitBoard.getCircuit().getName(), actualBits, wantBits));
        }

        InputPin q = new InputPin(substitutePin(reg.getPort(Register.PORT_OUT), true), this);
        OutputPin d = new OutputPin(substitutePin(reg.getPort(Register.PORT_IN), false), this);
        OutputPin en = new OutputPin(substitutePin(reg.getPort(Register.PORT_ENABLE), false), this);
        OutputPin clk = new OutputPin(substitutePin(reg.getPort(Register.PORT_CLK), false), this);
        OutputPin rst = new OutputPin(substitutePin(reg.getPort(Register.PORT_ZERO), false), this);

        return new MockRegister(q, d, en, clk, rst, this);
    }

    private Collection<Port> makeOrphanPort(Port port) {
        Collection<Port> connectedPorts = port.getLink().getParticipants().stream()
                                              .filter(p -> p != port).collect(Collectors.toList());
        connectedPorts.stream().forEach(port::unlinkPort);
        return connectedPorts;
    }

    private Pin substitutePin(Port port, boolean isInput) {
        Collection<Port> originallyConnectedPorts = makeOrphanPort(port);

        PinPeer mockPinPeer = new PinPeer(new Properties(
            new Properties.Property<>(Properties.BITSIZE, port.getLink().getBitSize()),
            new Properties.Property<>(PinPeer.IS_INPUT, isInput)
        ), 0, 0);

        // Find a valid place to drop it
        while (!circuitBoard.isValidLocation(mockPinPeer)) {
            mockPinPeer.setX(mockPinPeer.getX() + 32);
            mockPinPeer.setY(mockPinPeer.getY() + 32);
        }

        circuitBoard.addComponent(mockPinPeer);

        Pin mockPin = mockPinPeer.getComponent();
        Port newPort = mockPin.getPort(Pin.PORT);
        // In case CircuitSim automatically connected this new Pin to
        // anything, disconnect it
        makeOrphanPort(newPort);
        // Now reconnect all the old stuff
        originallyConnectedPorts.stream().forEach(newPort::linkPort);

        return mockPin;
    }
}
